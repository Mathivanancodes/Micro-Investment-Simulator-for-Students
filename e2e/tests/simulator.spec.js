import { randomBytes } from 'node:crypto';
import { test, expect } from '@playwright/test';

const currency = (amount) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(amount);

function newAccount() {
  const suffix = randomBytes(8).toString('hex');
  return { name: `Browser Student ${suffix}`, email: `browser-${suffix}@example.test`, password: randomBytes(20).toString('hex') };
}

async function register(page) {
  const account = newAccount();
  await page.goto('/');
  await page.getByRole('button', { name: 'Create Account', exact: true }).click();
  await page.getByLabel('Name', { exact: true }).fill(account.name);
  await page.getByLabel('Email address', { exact: true }).fill(account.email);
  await page.getByLabel('Password', { exact: true }).fill(account.password);
  const registration = page.waitForResponse((response) => response.url().endsWith('/api/auth/register') && response.request().method() === 'POST');
  await page.getByRole('button', { name: 'Create Wallet', exact: true }).click();
  const response = await registration;
  expect(response.status()).toBe(201);
  const identity = await response.json();
  await expect(page.getByRole('heading', { name: 'Your Investment Dashboard' })).toBeVisible();
  return { ...account, id: identity.userId, token: identity.token };
}

async function api(page, account, path, method = 'GET', data) {
  const response = await page.request.fetch(path, {
    method,
    headers: { Authorization: `Bearer ${account.token}` },
    ...(data === undefined ? {} : { data }),
  });
  expect(response.ok(), `${method} ${path}: HTTP ${response.status()}`).toBeTruthy();
  return response.json();
}

async function stocks(page, account) {
  return api(page, account, '/api/stocks');
}

async function buySetup(page, account, amount = 100) {
  const stock = (await stocks(page, account)).find((item) => item.symbol === 'NVDA');
  expect(stock).toBeTruthy();
  const trade = await api(page, account, '/api/trade/buy', 'POST', { userId: account.id, stockId: stock.id, amount });
  return { stock, trade };
}

async function openTrade(page, symbol = 'NVDA') {
  await page.getByRole('link', { name: 'Trade Stocks', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Stock Market Board' })).toBeVisible();
  await page.getByRole('button', { name: new RegExp(`^${symbol}\\b`) }).click();
  await expect(page.getByRole('heading', { name: symbol, exact: true })).toBeVisible();
}

test.beforeEach(async ({ page }) => {
  page.__applicationErrors = [];
  page.on('pageerror', (error) => page.__applicationErrors.push(error.message));
});

test.afterEach(async ({ page }) => {
  expect(page.__applicationErrors, 'No uncaught application errors').toEqual([]);
});

test('registration creates a virtual ₹1,000 wallet and an empty portfolio', async ({ page }) => {
  const account = await register(page);
  const cashCard = page.locator('.stat-card').filter({ hasText: 'Available Cash (Wallet)' });
  await expect(cashCard.getByRole('heading')).toHaveText(currency(1000));
  await expect(page.getByRole('region', { name: 'Your holdings table' })).toContainText('Your first investment can be just ₹10');
  await expect(page.getByLabel('Portfolio value in INR over time')).toBeVisible();
  await expect(page.locator('.sidebar-footer')).toContainText(account.name);
  await page.getByRole('link', { name: 'Wallet', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Your Virtual Wallet' })).toBeVisible();
  await expect(page.getByLabel('Available balance', { exact: true })).toHaveText(currency(1000));
});

test('adding virtual funds updates the balance and renders a deposit without stock fields', async ({ page }) => {
  const account = await register(page);
  await page.getByRole('link', { name: 'Wallet', exact: true }).click();
  await page.getByLabel('Deposit amount (INR)').fill('250.50');
  await page.getByRole('button', { name: 'Add virtual funds', exact: true }).click();
  await expect(page.getByRole('status')).toContainText('virtual funds added');
  await expect(page.getByLabel('Available balance', { exact: true })).toHaveText(currency(1250.5));
  await page.getByRole('link', { name: 'View transaction history', exact: true }).click();
  const deposit = page.getByRole('row').filter({ hasText: 'DEPOSIT' });
  await expect(deposit).toContainText('Wallet');
  await expect(deposit).toContainText(currency(250.5));
  await expect(deposit.getByRole('cell', { name: '—', exact: true })).toHaveCount(2);
  const ledger = await api(page, account, `/api/transactions/${account.id}`);
  expect(ledger.totalElements).toBe(1);
  expect(ledger.content[0].type).toBe('DEPOSIT');
});

test('buying by rupee amount executes fractional shares and updates holdings and cash', async ({ page }) => {
  const account = await register(page);
  await openTrade(page);
  await expect(page.getByRole('heading', { name: '7-Day Price History' })).toBeVisible();
  await expect(page.getByLabel('Price per share in INR over time')).toBeVisible();
  await page.getByLabel('Investment amount (INR)').fill('10.00');
  const execution = page.waitForResponse((response) => response.url().endsWith('/api/trade/buy') && response.request().method() === 'POST');
  await page.getByRole('button', { name: 'Buy shares', exact: true }).click();
  const result = await execution;
  expect(result.status()).toBe(200);
  const trade = await result.json();
  expect(Number(trade.quantity)).toBeGreaterThan(0);
  expect(Number(trade.quantity)).toBeLessThan(1);
  expect(Number(trade.quantity) * 10000).toBeCloseTo(Math.round(Number(trade.quantity) * 10000), 8);
  expect(Number(trade.amount)).toBe(10);
  expect(Number(trade.remainingBalance)).toBe(990);
  await expect(page.getByRole('status').filter({ hasText: 'Successfully bought' })).toContainText(`${Number(trade.quantity).toFixed(4)} shares of NVDA`);
  await page.getByRole('link', { name: 'Dashboard', exact: true }).click();
  const holding = page.getByRole('region', { name: 'Your holdings table' }).getByRole('row').filter({ hasText: 'NVDA' });
  await expect(holding).toContainText(Number(trade.quantity).toFixed(4));
  await expect(page.locator('.stat-card').filter({ hasText: 'Available Cash (Wallet)' }).getByRole('heading')).toHaveText(currency(990));
  const portfolio = await api(page, account, `/api/portfolio/${account.id}`);
  expect(Number(portfolio.holdings[0].quantity)).toBe(Number(trade.quantity));
});

test('overselling displays the server error and preserves wallet, holdings, and ledger', async ({ page }) => {
  const account = await register(page);
  const { stock, trade } = await buySetup(page, account);
  await openTrade(page);
  await page.getByRole('button', { name: 'Sell', exact: true }).click();
  await page.getByLabel('Quantity to sell (shares)').fill((Number(trade.quantity) + 1).toFixed(4));
  await page.getByRole('button', { name: 'Sell shares', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText(/insufficient|enough|exceed/i);
  const portfolio = await api(page, account, `/api/portfolio/${account.id}`);
  expect(Number(portfolio.walletBalance)).toBe(900);
  expect(Number(portfolio.holdings.find((holding) => holding.stockId === stock.id).quantity)).toBe(Number(trade.quantity));
  const ledger = await api(page, account, `/api/transactions/${account.id}`);
  expect(ledger.totalElements).toBe(1);
});

test('a partial sale preserves cost basis and credits its executed proceeds', async ({ page }) => {
  const account = await register(page);
  const { stock, trade: purchase } = await buySetup(page, account);
  const before = await api(page, account, `/api/portfolio/${account.id}`);
  const soldQuantity = Math.floor(Number(purchase.quantity) * 10000 / 2) / 10000;
  await openTrade(page);
  await page.getByRole('button', { name: 'Sell', exact: true }).click();
  await page.getByLabel('Quantity to sell (shares)').fill(soldQuantity.toFixed(4));
  const execution = page.waitForResponse((response) => response.url().endsWith('/api/trade/sell') && response.request().method() === 'POST');
  await page.getByRole('button', { name: 'Sell shares', exact: true }).click();
  const result = await execution;
  expect(result.status()).toBe(200);
  const sale = await result.json();
  await expect(page.getByRole('status').filter({ hasText: 'Successfully sold' })).toContainText(currency(sale.amount));
  const after = await api(page, account, `/api/portfolio/${account.id}`);
  const holding = after.holdings.find((item) => item.stockId === stock.id);
  expect(Number(holding.quantity)).toBeCloseTo(Number(purchase.quantity) - soldQuantity, 4);
  expect(Number(holding.averagePrice)).toBe(Number(before.holdings[0].averagePrice));
  expect(Number(after.walletBalance)).toBeCloseTo(900 + Number(sale.amount), 2);
  await page.getByRole('link', { name: 'Dashboard', exact: true }).click();
  await expect(page.getByRole('region', { name: 'Your holdings table' })).toContainText(Number(holding.quantity).toFixed(4));
});

test('transaction pages show the correct rows and preserve descending chronology', async ({ page }) => {
  const account = await register(page);
  for (let amount = 1; amount <= 7; amount++) {
    await api(page, account, `/api/wallet/${account.id}/deposit`, 'POST', { amount });
  }
  await page.getByRole('link', { name: 'History', exact: true }).click();
  await page.getByLabel('Rows per page').selectOption('5');
  await expect(page.getByRole('status')).toHaveText('Page 1 of 2');
  let rows = page.getByRole('region', { name: 'Transaction history table' }).locator('tbody tr');
  await expect(rows).toHaveCount(5);
  await expect(rows.first()).toContainText(currency(7));
  await expect(page.getByRole('button', { name: 'Previous', exact: true })).toBeDisabled();
  await page.getByRole('button', { name: 'Next', exact: true }).click();
  await expect(page.getByRole('status')).toHaveText('Page 2 of 2');
  rows = page.getByRole('region', { name: 'Transaction history table' }).locator('tbody tr');
  await expect(rows).toHaveCount(2);
  await expect(rows.first()).toContainText(currency(2));
  await expect(rows.last()).toContainText(currency(1));
  await expect(page.getByRole('button', { name: 'Next', exact: true })).toBeDisabled();
  await page.getByRole('button', { name: 'Previous', exact: true }).click();
  await expect(page.getByRole('status')).toHaveText('Page 1 of 2');
});

test('leaderboard identifies the signed-in student and excludes deposits from investment returns', async ({ page }) => {
  const account = await register(page);
  await api(page, account, `/api/wallet/${account.id}/deposit`, 'POST', { amount: 500 });
  await page.getByRole('link', { name: 'Leaderboard', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Classroom Leaderboard' })).toBeVisible();
  const current = page.getByRole('region', { name: 'Student rankings table' }).getByRole('row').filter({ hasText: account.name });
  await expect(current).toContainText('YOU');
  await expect(current).toContainText(currency(1500));
  await expect(current).toContainText('+0.00%');
  await expect(current.getByRole('cell').last()).toHaveText('0');
});

test('reload retains the session and logout/login restores the same wallet', async ({ page }) => {
  const account = await register(page);
  await api(page, account, `/api/wallet/${account.id}/deposit`, 'POST', { amount: 23.45 });
  await page.reload();
  await expect(page.getByRole('heading', { name: 'Your Investment Dashboard' })).toBeVisible();
  await expect(page.locator('.stat-card').filter({ hasText: 'Available Cash (Wallet)' }).getByRole('heading')).toHaveText(currency(1023.45));
  await page.getByRole('button', { name: 'Sign Out', exact: true }).click();
  await expect(page.getByLabel('Email address', { exact: true })).toBeVisible();
  expect(await page.evaluate(() => localStorage.getItem('token'))).toBeNull();
  await page.getByLabel('Email address', { exact: true }).fill(account.email);
  await page.getByLabel('Password', { exact: true }).fill(account.password);
  await page.locator('form').getByRole('button', { name: 'Sign In', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Your Investment Dashboard' })).toBeVisible();
  await expect(page.locator('.stat-card').filter({ hasText: 'Available Cash (Wallet)' }).getByRole('heading')).toHaveText(currency(1023.45));
  expect(await page.evaluate(() => localStorage.getItem('userId'))).toBe(String(account.id));
});

test('an invalid saved token returns to sign-in and clears the session', async ({ page }) => {
  await register(page);
  await page.evaluate(() => localStorage.setItem('token', 'invalid.token.signature'));
  await page.reload();
  await expect(page.getByLabel('Email address', { exact: true })).toBeVisible();
  expect(await page.evaluate(() => localStorage.getItem('token'))).toBeNull();
  expect(await page.evaluate(() => localStorage.getItem('userId'))).toBeNull();
});

test('investment inputs reject out-of-range amounts and excess decimal places before submission', async ({ page }) => {
  const account = await register(page);
  await openTrade(page);
  const amount = page.getByLabel('Investment amount (INR)');
  for (const invalid of ['9.99', '500.01', '100.001']) {
    await amount.fill(invalid);
    expect(await amount.evaluate((input) => input.checkValidity())).toBe(false);
    await page.getByRole('button', { name: 'Buy shares', exact: true }).click();
    expect((await api(page, account, `/api/transactions/${account.id}`)).totalElements).toBe(0);
  }
  await amount.fill('500.00');
  expect(await amount.evaluate((input) => input.checkValidity())).toBe(true);
  await amount.fill('10.00');
  expect(await amount.evaluate((input) => input.checkValidity())).toBe(true);
});

test('market search and recoverable API failures show usable empty and retry states', async ({ page }) => {
  const account = await register(page);
  await openTrade(page);
  await page.getByLabel('Search stocks', { exact: true }).fill('NO_SUCH_COMPANY_12345');
  await expect(page.getByText('No stocks match your search.', { exact: true })).toBeVisible();
  await page.getByLabel('Search stocks', { exact: true }).fill('NVDA');
  await expect(page.getByRole('button', { name: /^NVDA\b/ })).toHaveCount(1);
  const route = `**/api/wallet/${account.id}`;
  await page.route(route, (request) => request.abort('failed'));
  await page.getByRole('link', { name: 'Wallet', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Unable to load your wallet' })).toBeVisible();
  await expect(page.getByRole('alert')).toContainText(/reach the server/i);
  await page.unroute(route);
  await page.getByRole('button', { name: 'Try again', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Your Virtual Wallet' })).toBeVisible();
  await expect(page.getByLabel('Available balance', { exact: true })).toHaveText(currency(1000));
});

for (const width of [375, 768]) {
  test(`all primary pages remain usable at ${width}px without document overflow`, async ({ page }) => {
    await page.setViewportSize({ width, height: 900 });
    await register(page);
    for (const [link, heading] of [
      ['Dashboard', 'Your Investment Dashboard'],
      ['Wallet', 'Your Virtual Wallet'],
      ['Trade Stocks', 'Stock Market Board'],
      ['History', 'Transaction History'],
      ['Leaderboard', 'Classroom Leaderboard'],
    ]) {
      await page.getByRole('link', { name: link, exact: true }).click();
      await expect(page.getByRole('heading', { name: heading, exact: true })).toBeVisible();
      const dimensions = await page.evaluate(() => ({ viewport: document.documentElement.clientWidth, content: document.documentElement.scrollWidth }));
      expect(dimensions.content, `${link} document width`).toBeLessThanOrEqual(dimensions.viewport + 1);
    }
  });
}

test('same-origin API blocks cross-user access and returns structured precision/malformed errors', async ({ page }) => {
  const account = await register(page);
  const other = newAccount();
  const registration = await page.request.post('/api/auth/register', { data: other });
  expect(registration.status()).toBe(201);
  const owner = await registration.json();
  const stock = (await stocks(page, account))[0];
  const headers = { Authorization: `Bearer ${account.token}` };
  for (const path of [`/api/wallet/${owner.userId}`, `/api/portfolio/${owner.userId}`, `/api/transactions/${owner.userId}`]) {
    const denied = await page.request.get(path, { headers });
    expect(denied.status()).toBe(403);
    expect((await denied.json()).path).toBe(path);
  }
  const forbiddenTrade = await page.request.post('/api/trade/buy', { headers, data: { userId: owner.userId, stockId: stock.id, amount: 100 } });
  expect(forbiddenTrade.status()).toBe(403);
  for (const [path, data] of [
    ['/api/trade/buy', { userId: account.id, stockId: stock.id, amount: 10.001 }],
    ['/api/trade/sell', { userId: account.id, stockId: stock.id, quantity: 0.00001 }],
    [`/api/wallet/${account.id}/deposit`, { amount: 0.001 }],
  ]) {
    const invalid = await page.request.post(path, { headers, data });
    expect(invalid.status()).toBe(400);
    const body = await invalid.json();
    expect(body).toMatchObject({ status: 400, path });
    for (const field of ['timestamp', 'error', 'message']) expect(body[field]).toBeTruthy();
  }
  const malformed = await page.request.post('/api/trade/buy', { headers: { ...headers, 'Content-Type': 'application/json' }, data: '{broken' });
  expect(malformed.status()).toBe(400);
  expect(await malformed.json()).toMatchObject({ status: 400, path: '/api/trade/buy' });
  const wallet = await api(page, account, `/api/wallet/${account.id}`);
  expect(Number(wallet.balance)).toBe(1000);
  expect((await api(page, account, `/api/transactions/${account.id}`)).totalElements).toBe(0);
});
