# Browser checks

The Playwright suite exercises the real React UI, Vite API proxy, Spring Boot backend, and MySQL database. It creates uniquely named test users and virtual transactions. Run it against a development or disposable database; it preserves all existing accounts and does not delete test data.

Start the application from the repository root:

```bash
./scripts/dev.sh
```

In another terminal:

```bash
cd e2e
npm ci
npx playwright install chromium
npm test
```

If Chromium is already installed on the system:

```bash
cd e2e
npm ci
CHROMIUM_PATH=/usr/bin/chromium npm test
```

`E2E_BASE_URL` overrides the frontend URL (default `http://127.0.0.1:5173`). API requests use that frontend's `/api` proxy, so they exercise the same backend as the browser. The config reuses a running frontend or starts Vite automatically; the backend and database must already be ready. To test a separate backend, set `VITE_PROXY_TARGET` when starting Vite.

Tests cover registration and initial funding, deposits, fractional purchases, partial sales, rejected overselling, transaction pagination, deposit rendering, leaderboard returns, session persistence, logout/login, expired sessions, input validation, retry states, stock search, mobile layouts, ownership enforcement, financial precision, malformed JSON, and uncaught browser errors. Executed server prices determine financial assertions so scheduled price updates do not make tests flaky.

There are no retries or skipped tests. Failures retain screenshots and traces in ignored `test-results/`; the HTML report is in ignored `playwright-report/`. Open it with `npm run report`. These local artifacts can include the randomly generated test credentials and must stay out of Git.
