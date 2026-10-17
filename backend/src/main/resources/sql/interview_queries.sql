-- MySQL 8.0+ interview examples. Run against a local development database.
-- This file is a query workbook, not a schema migration or a startup initializer.
USE micro_investment;
-- Set these session variables before sourcing the workbook to select an isolated test account.
SET @example_user_id = COALESCE(@example_user_id, (SELECT MIN(id) FROM users));
SET @example_stock_id = COALESCE(@example_stock_id, (SELECT MIN(id) FROM stocks));
SET @minimum_holdings = 2;

-- 1. INNER JOIN: only users who currently own at least one stock.
SELECT u.id AS user_id, u.name, s.symbol, h.quantity, h.avg_buy_price,
       s.current_price, ROUND(h.quantity * s.current_price, 2) AS market_value
FROM holdings h
INNER JOIN users u ON u.id = h.user_id
INNER JOIN stocks s ON s.id = h.stock_id
ORDER BY u.id, s.symbol;

-- 2. LEFT JOIN: include users with no holdings. COALESCE turns an empty sum into 0.
SELECT u.id AS user_id, u.name, COUNT(h.id) AS holding_count,
       COALESCE(SUM(ROUND(h.quantity * s.current_price, 2)), 0) AS portfolio_value
FROM users u
LEFT JOIN holdings h ON h.user_id = u.id
LEFT JOIN stocks s ON s.id = h.stock_id
GROUP BY u.id, u.name
ORDER BY u.id;

-- 3. GROUP BY + HAVING: current invested cost, for users with more than N positions.
-- WHERE filters rows before grouping; HAVING filters the resulting groups.
SELECT u.id AS user_id, u.name, COUNT(h.id) AS holding_count,
       SUM(ROUND(h.quantity * h.avg_buy_price, 2)) AS current_invested_cost
FROM users u
INNER JOIN holdings h ON h.user_id = u.id
GROUP BY u.id, u.name
HAVING COUNT(h.id) > @minimum_holdings
ORDER BY current_invested_cost DESC;

-- 4. Aggregates: market value and cost basis for a single current portfolio.
SELECT h.user_id,
       SUM(ROUND(h.quantity * s.current_price, 2)) AS portfolio_value,
       SUM(ROUND(h.quantity * h.avg_buy_price, 2)) AS invested_cost,
       SUM(ROUND(h.quantity * s.current_price, 2))
           - SUM(ROUND(h.quantity * h.avg_buy_price, 2)) AS unrealized_pnl
FROM holdings h
INNER JOIN stocks s ON s.id = h.stock_id
WHERE h.user_id = @example_user_id
GROUP BY h.user_id;

-- 5. AVG demonstrates an unweighted mean; weighted mean is appropriate for shares.
SELECT t.stock_id, s.symbol,
       ROUND(AVG(t.price), 2) AS average_trade_price,
       ROUND(SUM(t.quantity * t.price) / NULLIF(SUM(t.quantity), 0), 2)
           AS quantity_weighted_buy_price,
       SUM(t.amount) AS historical_buy_amount
FROM transactions t
INNER JOIN stocks s ON s.id = t.stock_id
WHERE t.type = 'BUY'
GROUP BY t.stock_id, s.symbol
ORDER BY s.symbol;

-- 6. Scalar subquery: users whose wallet exceeds the mean wallet balance.
SELECT u.id, u.name, w.balance
FROM users u
INNER JOIN wallets w ON w.user_id = u.id
WHERE w.balance > (SELECT AVG(balance) FROM wallets)
ORDER BY w.balance DESC;

-- 7. Window function: capital-adjusted leaderboard with ties sharing a rank.
-- Opening balance is 1000, not a DEPOSIT transaction. Later deposits add to baseline.
-- Aggregate holdings and deposits separately before joining to avoid multiplying rows.
WITH holding_values AS (
    SELECT h.user_id, SUM(ROUND(h.quantity * s.current_price, 2)) AS market_value
    FROM holdings h
    INNER JOIN stocks s ON s.id = h.stock_id
    GROUP BY h.user_id
), deposits AS (
    SELECT user_id, SUM(amount) AS deposited
    FROM transactions
    WHERE type = 'DEPOSIT'
    GROUP BY user_id
), performance AS (
    SELECT u.id AS user_id, u.name,
           w.balance + COALESCE(hv.market_value, 0) AS total_wealth,
           1000.00 + COALESCE(d.deposited, 0) AS capital_baseline,
           100.00 * (w.balance + COALESCE(hv.market_value, 0)
                        - 1000.00 - COALESCE(d.deposited, 0))
               / (1000.00 + COALESCE(d.deposited, 0)) AS pnl_percentage
    FROM users u
    INNER JOIN wallets w ON w.user_id = u.id
    LEFT JOIN holding_values hv ON hv.user_id = u.id
    LEFT JOIN deposits d ON d.user_id = u.id
)
SELECT RANK() OVER (ORDER BY pnl_percentage DESC) AS leaderboard_rank,
       user_id, name, ROUND(total_wealth, 2) AS total_wealth,
       ROUND(capital_baseline, 2) AS capital_baseline,
       ROUND(total_wealth - capital_baseline, 2) AS pnl,
       ROUND(pnl_percentage, 4) AS pnl_percentage
FROM performance
ORDER BY leaderboard_rank, user_id;

-- 8. LAG: chronological changes for one stock; id makes timestamp ties deterministic.
WITH steps AS (
    SELECT id, stock_id, recorded_at, price,
           LAG(price) OVER (PARTITION BY stock_id ORDER BY recorded_at, id) AS previous_price
    FROM price_history
    WHERE stock_id = @example_stock_id
)
SELECT recorded_at, price, previous_price,
       price - previous_price AS price_change,
       ROUND(100 * (price - previous_price) / NULLIF(previous_price, 0), 4) AS change_percent
FROM steps
ORDER BY recorded_at, id;

-- 9. Top five gainers: compare earliest and latest available samples in the last 24h.
-- Two samples are required; a new database has no full-day history yet.
WITH samples AS (
    SELECT id, stock_id, price, recorded_at,
           ROW_NUMBER() OVER (PARTITION BY stock_id ORDER BY recorded_at, id) AS first_row,
           ROW_NUMBER() OVER (PARTITION BY stock_id ORDER BY recorded_at DESC, id DESC) AS last_row,
           COUNT(*) OVER (PARTITION BY stock_id) AS sample_count
    FROM price_history
    WHERE recorded_at >= UTC_TIMESTAMP() - INTERVAL 24 HOUR
), changes AS (
    SELECT stock_id,
           MAX(CASE WHEN first_row = 1 THEN price END) AS opening_price,
           MAX(CASE WHEN last_row = 1 THEN price END) AS latest_price,
           MAX(sample_count) AS sample_count
    FROM samples
    GROUP BY stock_id
)
SELECT s.symbol, s.name, c.opening_price, c.latest_price,
       ROUND(100 * (c.latest_price - c.opening_price) / c.opening_price, 4) AS gain_percent
FROM changes c
INNER JOIN stocks s ON s.id = c.stock_id
WHERE c.sample_count >= 2 AND c.latest_price > c.opening_price
ORDER BY gain_percent DESC, s.symbol
LIMIT 5;

-- 10. Index usage: stock equality followed by a time range matches the composite index.
-- Inspect key, type, rows, and Extra. The optimizer may scan a tiny demonstration table.
EXPLAIN
SELECT id, stock_id, price, recorded_at
FROM price_history
WHERE stock_id = @example_stock_id
  AND recorded_at >= UTC_TIMESTAMP() - INTERVAL 24 HOUR
ORDER BY recorded_at DESC
LIMIT 100;

-- 11. Transaction index example: leading user_id then recent chronological activity.
EXPLAIN
SELECT id, type, quantity, price, amount, created_at
FROM transactions
WHERE user_id = @example_user_id
ORDER BY created_at DESC
LIMIT 20;

-- 12. ACID / isolation / optimistic version illustration. These statements deliberately
-- roll back and do not represent the full trade algorithm; the service also updates the
-- holding and inserts its transaction row before committing. Use a development database.
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;
SELECT balance, version INTO @old_balance, @old_version
FROM wallets WHERE user_id = @example_user_id;
UPDATE wallets
SET balance = balance - 10.00, version = version + 1
WHERE user_id = @example_user_id AND version = @old_version AND balance >= 10.00;
SELECT ROW_COUNT() AS updated_rows;
-- A zero count means the balance/version precondition failed; the application aborts.
-- A successful buy must debit, alter its holding, and insert BUY inside this same unit.
ROLLBACK;

-- Normalization: users identify people; wallets track cash; stocks describe instruments;
-- holdings summarize current positions; transactions preserve operations; price_history
-- stores time-series facts. Foreign keys avoid duplicated identity and stock descriptions.
-- Current price and average acquisition price are deliberate transactionally maintained
-- summaries. A real-money system would additionally use an audited double-entry ledger.
