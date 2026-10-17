# Verification record

Locally verified on 2026-10-17 using Java 17, Maven 3.9.9, Node 24, MySQL 8.4, and Chromium.

| Check | Result |
| --- | --- |
| `./scripts/check.sh`: backend unit and security tests | 37 passed |
| Same command: real MySQL integration tests | 12 passed |
| Backend total | 49 passed, 0 failed, 0 errors, 0 skipped |
| Frontend production build | Passed without warnings |
| Frontend lint | Passed without warnings |
| `cd e2e && CHROMIUM_PATH=/usr/bin/chromium npm test` | 14 passed, 0 skipped, 0 retries |
| Complete executable sequence in `API_TESTS.md` | 42 HTTP status/behavior assertions passed |
| Documented parallel-deposit check | 3 commits, 21 HTTP 409 conflicts; balance and transaction counts agreed |
| Entire `interview_queries.sql` workbook | Executed successfully on MySQL; final example rolled back |
| Application restart | Healthy backend, connected database, preserved seed data, and working frontend |

Backend checks include authorization for every private account operation, malformed JSON and financial precision validation, cash/holding/ledger atomicity, weighted average price, actual Hibernate optimistic-lock conflicts, simultaneous purchases, detached entity identity, deposits, pagination, CSV export, and SQL ranking. Price-strategy and scheduler tests check the simulated-price path.

Browser checks use the actual React application and its API proxy. They cover initial funding, deposits, amount-based fractional purchases, partial sales, rejected overselling, charts, history paging, leaderboard returns, stock search, error recovery, session persistence/expiry, logout/login, and layouts at desktop, 375px and 768px widths. No external font request is required.

Tests create isolated accounts with virtual transactions. Database integration tests use the disposable `micro_investment_test` schema; development accounts are retained. Generated local credentials, browser reports, traces, and build outputs are ignored by Git.

The GitHub Actions workflow runs backend, database, frontend, and browser checks on subsequent pushes and pull requests. These results record the local verification; a remote Actions result is reported separately by GitHub.
