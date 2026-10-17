import { defineConfig, devices } from '@playwright/test';

const baseURL = process.env.E2E_BASE_URL || 'http://127.0.0.1:5173';
const origin = new URL(baseURL);

export default defineConfig({
  testDir: './tests',
  fullyParallel: false,
  workers: 1,
  timeout: 60000,
  expect: { timeout: 15000 },
  forbidOnly: Boolean(process.env.CI),
  retries: 0,
  reporter: [['list'], ['html', { open: 'never' }], ['json', { outputFile: 'test-results/results.json' }]],
  use: {
    baseURL,
    actionTimeout: 15000,
    navigationTimeout: 30000,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'off',
    locale: 'en-IN',
    timezoneId: 'Asia/Kolkata',
    launchOptions: process.env.CHROMIUM_PATH ? { executablePath: process.env.CHROMIUM_PATH } : {},
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'], viewport: { width: 1440, height: 1000 } } }],
  // The API/MySQL lifecycle remains with scripts/dev.sh; tests never create or reset a database.
  webServer: {
    command: `npm --prefix ../frontend run dev -- --host ${origin.hostname} --port ${origin.port || '5173'} --strictPort`,
    url: baseURL,
    timeout: 60000,
    reuseExistingServer: true,
  },
});
