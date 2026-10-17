# Micro-Investment Simulator for Students

Practise fractional-share investing with virtual INR. Register for a ₹1,000 wallet, invest ₹10–₹500 per buy, sell shares, add virtual cash, and compare portfolio performance. Prices are simulated; no real money or external market-data API is involved.

The project uses Java 17, Spring Boot 3, Maven, MySQL 8.4, React, and Vite. It demonstrates transaction boundaries, optimistic concurrency, validation, JWT authentication, Java polymorphism, and SQL reporting in a beginner-readable structure.

## Run locally

Install JDK 17, Maven 3.9+, Node.js 24 with npm 11, Docker with Compose v2, OpenSSL, and curl. Docker must be running. From the repository root:

```bash
./scripts/dev.sh
```

The script generates local credentials and a JWT signing secret, starts MySQL, configures a trust store for its local certificate, installs frontend dependencies from the lockfile, builds the backend, and starts both development servers. The initial dependency downloads can take several minutes.

- Frontend: [http://localhost:5173](http://localhost:5173)
- Backend health: [http://localhost:8080/api/health](http://localhost:8080/api/health)
- API base: `http://localhost:8080/api`
- Local database: `127.0.0.1:3307`, schema `micro_investment`

The Vite development server forwards `/api` requests to the backend. Press Ctrl-C in the terminal to stop the application servers. MySQL and its named volume remain available so that restarting preserves accounts and trades. Application logs are `.local/backend.log` and `.local/frontend.log`.

Register your own account through the UI. The development profile also seeds Asha Student (`student1@example.test`), Ravi Student (`student2@example.test`), and eight stocks: AAPL, MSFT, GOOGL, NVDA, AMZN, TSLA, DIS, and WMT. Demo passwords are randomly generated in the ignored `.local/dev.env` file under `DEMO_PASSWORD`; use that file locally if you need the seeded accounts, and never publish it. The initial historical samples are synthetic fixtures for chart demonstrations. Seeding is repeatable and does not reset existing balances or passwords.

To stop the database while keeping its data:

```bash
source scripts/lib.sh
compose down
```

Do not delete `.local/dev.env` while retaining the database volume: MySQL retains the original credentials. Keep local files out of Git. Changing `LOCAL_DB_PORT` before startup selects another host port if 3307 is occupied.

## Verify the project

With Docker running, use the repository check command:

```bash
./scripts/check.sh
```

It runs the backend unit tests and real MySQL integration tests, then installs frontend dependencies and runs its production build and lint. Integration tests use the separate disposable `micro_investment_test` schema; do not point that schema at valuable data. A passing build does not replace database and browser verification.

Backend-only unit tests can run without MySQL:

```bash
cd backend
mvn -B -ntp test
```

Follow [API_TESTS.md](docs/API_TESTS.md) for a complete executable cURL sequence, including registration, login, deposits, buys, partial/full sells, pagination, ownership checks, invalid input, and parallel wallet updates. It uses generated test credentials and verifies balances against committed transaction counts. Its browser checklist covers login, charts, trading, responsive layout, and error messages.

For automated browser checks, keep `./scripts/dev.sh` running and open another terminal:

```bash
cd e2e
npm ci
npx playwright install chromium
npm test
```

The [Playwright guide](e2e/README.md) explains system Chromium, test URLs, reports, and test-data handling. The suite exercises the real frontend, API proxy, backend, and database. The [CI workflow](.github/workflows/check.yml) runs backend tests, frontend build/lint, and these browser workflows on pushes and pull requests.

## Repository layout

```text
backend/
  pom.xml
  src/main/java/com/microinvestment/simulator/
    SimulatorApplication.java
    config/        JWT, security, CORS, and strategy wiring
    controller/    HTTP endpoints and validated DTO entry points
    dto/           Request, response, and paginated API contracts
    entity/        Six JPA tables and shared BaseEntity
    exception/     Domain errors and consistent HTTP error handling
    repository/    Persistence, fetch queries, and SQL projections
    scheduler/     Simulated price updates every 30 seconds
    service/       Authentication, wallets, trades, and reports
    strategy/      PriceStrategy, RandomWalkStrategy, TrendStrategy
  src/main/resources/
    application.properties
    application-dev.properties
    sql/interview_queries.sql
  src/test/        Business-rule and database integration tests
frontend/
  src/components/ Reusable UI components
  src/pages/      Authentication, dashboard, trade, history, leaderboard
  src/services/   Shared API client
e2e/              Playwright browser workflows and reports configuration
scripts/          Repeatable startup and validation
docs/             Architecture and executable API checks
compose.yaml      MySQL 8.4 development service
root.sql          Optional development schema creation
INTERVIEW_NOTES.md
```

## Business rules

All amounts represent virtual INR. Registration creates one wallet with ₹1,000. Buys accept ₹10–₹500 with at most two decimal places; share quantity is amount divided by current price, rounded to four decimal places with `HALF_UP`. The exact requested amount is debited. Sales accept at most four quantity decimals and credit proceeds rounded to two money decimals. Deposits are positive amounts with at most two decimals. Money uses `BigDecimal` in Java and decimal columns in MySQL.

Each buy or sell atomically updates the wallet, position, and immutable transaction record. A wallet version detects concurrent changes and returns HTTP 409 to the stale request. Insufficient funds, insufficient shares, and invalid requests return 400. The API rejects access to another user's wallet, portfolio, history, or trades even when the caller has a valid token.

Portfolio P&L measures unrealized gains on current holdings: market value minus their acquisition cost. The leaderboard includes both cash and holdings, comparing total wealth with the original ₹1,000 plus deposits. Depositing money therefore does not count as investment profit. Fractional quantity rounding can produce a small marked-value difference immediately after a purchase.

The price scheduler records a new simulated price every 30 seconds. Random-walk prices move by up to ±2% per tick, and an alternative trend strategy demonstrates polymorphism. Stock charts use recorded history, not an external finance provider.

## Configuration

| Variable | Purpose | Default |
| --- | --- | --- |
| `JWT_SECRET` | At least 32 bytes of unpredictable signing material | Required; local script generates it |
| `SPRING_DATASOURCE_URL` | Complete JDBC URL, overriding the standard host configuration | Local script configures trusted MySQL TLS |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | Database account | Local script supplies generated credentials |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | Alternative database configuration | `localhost`, `3306`, `micro_investment`, `simulator`, no password |
| `PORT` | Backend HTTP port | `8080` |
| `CORS_ORIGINS` | Comma-separated allowed frontend origins | Localhost and 127.0.0.1 on port 5173 |
| `SEED_DEMO` | Enable optional demo fixtures outside the dev profile | `false` |
| `DEMO_PASSWORD` | Demo password, at least 12 characters when seeding | Local script generates it |
| `PRICE_STRATEGY` | `random` or `trend` | `random` |
| `PRICES_ENABLED` | Enable scheduled price ticks | `true` |
| `PRICE_INTERVAL_MS` | Price update interval | `30000` |
| `SHOW_SQL` | Log SQL for local debugging | `false` |
| `LOCAL_DB_PORT` | Host port used by the supplied Docker setup | `3307` |

The `dev` Spring profile explicitly enables fixtures. It is used by `scripts/dev.sh`. Without that profile, set `SEED_DEMO=true` and provide `DEMO_PASSWORD` only when demo data is wanted. The script reads generated credentials from `.local/dev.env`; runtime environments should inject their own secrets securely. Never commit secrets or hard-code them in Java.

For an existing cloud database, supply the datasource and JWT settings through the environment and use the same Maven and npm commands. The default JDBC URL verifies server identity. The local Docker helper trusts its generated CA using `VERIFY_CA`; hosted databases should use `VERIFY_IDENTITY` and a trust store appropriate to their certificate. Do not disable TLS or artifact verification to work around installation errors.

The default schema-management setting is `spring.jpa.hibernate.ddl-auto=update`, as required by this teaching project. Use reviewed migrations before a production deployment. If upgrading a database from an older version of this project, Hibernate does not migrate renamed columns or backfill incompatible data; use a fresh development schema or a separately reviewed data migration.

## Learn and extend

[Architecture](docs/ARCHITECTURE.md) explains transaction boundaries, authorization, normalization, calculations, isolation, and concurrency. [Interview notes](INTERVIEW_NOTES.md) contain 25 questions with short answers and a 60-second pitch. The [SQL workbook](backend/src/main/resources/sql/interview_queries.sql) contains joins, grouped aggregates, an average-balance subquery, `RANK`, `LAG`, top-five gainers in the last 24 hours, and `EXPLAIN` examples.

After starting the local development database, execute the SQL workbook without displaying credentials:

```bash
source scripts/lib.sh
compose exec -T db sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysql -u "$MYSQL_USER" "$MYSQL_DATABASE"' \
  < backend/src/main/resources/sql/interview_queries.sql
```

Its final transaction example explicitly rolls back its demonstration debit. The workbook is for development and is not run automatically at application startup.

The current design is a learning simulator. Before a public deployment, add versioned migrations, operational monitoring, backups, stronger session controls, rate limiting, and a single elected scheduler across API instances. Handling real money would require a different audited ledger and additional financial and operational controls.
