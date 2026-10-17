# Interview notes

## 60-second project pitch

“I built a micro-investment simulator where students practise buying fractional shares with virtual INR. It uses a React frontend, a Java 17 Spring Boot API, and MySQL. Registration creates a ₹1,000 wallet, and each purchase invests ₹10 to ₹500. I use BigDecimal for money and four decimal places for share quantities. The service layer makes the wallet update, holding change, and transaction record atomic. A version column on the wallet detects concurrent updates instead of silently losing money. A strategy interface lets the price scheduler switch between random-walk and trend implementations. I kept request DTOs separate from entities, added validation and consistent errors, and protected every user's portfolio and trades with ownership checks. The reporting SQL demonstrates joins, aggregates, window functions, and indexes. My tests cover normal trades, insufficient funds or holdings, invalid requests, authorization, and concurrency. The project taught me how financial rules, database transactions, and API contracts fit together.”

## 25 likely questions

1. **Why did you choose BigDecimal instead of double?** Binary floating-point cannot exactly represent many decimal amounts, such as 0.1. BigDecimal with explicit scale and rounding makes the money rules reproducible. Construct it from a string, not a double.

2. **How do fractional shares work?** A buy divides the INR amount by the current price, rounds to four decimal places with `HALF_UP`, and rejects a result that cannot represent a positive position. Money and prices use two decimal places. The requested amount is the cash debit; a small valuation difference can arise from quantity rounding.

3. **Why use @Transactional on the service?** A trade changes the wallet, holding, and transaction log. The service coordinates all three, so it is the correct transaction boundary. A failure before commit must roll everything back.

4. **What does optimistic locking prevent?** Two requests can read the same balance and both think they have enough money. JPA updates the wallet only when its version still matches, increments the version, and rejects a stale writer. The entire losing trade rolls back and returns 409.

5. **Why optimistic locking instead of pessimistic locking?** Most users trade independently, so conflicts should be uncommon. Optimistic locking avoids holding a database row lock while application code runs. Pessimistic locking is an alternative if contention becomes high, with more blocking and deadlock handling.

6. **What are the ACID properties in one buy?** Atomicity commits the debit, position, and log together. Consistency preserves balances, quantities, and foreign keys. Isolation combines committed reads with version checks. Durability comes from a successful MySQL commit and its configured transaction log.

7. **Why choose READ_COMMITTED?** A trade never reads another transaction's uncommitted state. The wallet version still matters because this isolation level does not automatically prevent lost updates. More restrictive isolation can cost concurrency without replacing business invariants.

8. **Why return DTOs instead of entities?** DTOs define the public API, hide password hashes, avoid serializing lazy JPA graphs, and allow request-specific validation. Database changes can then happen without accidentally changing every API response.

9. **Where is abstraction used?** `BaseEntity` is an abstract mapped superclass providing identity and creation time. It shares state and behavior but is not its own database table. Concrete entities extend it.

10. **How is an interface different from an abstract class here?** `PriceStrategy` declares a behavior contract and can be implemented by unrelated classes. `BaseEntity` shares entity state and implementation. Java allows multiple interfaces but one class superclass.

11. **Where are polymorphism and overriding used?** The scheduler calls `PriceStrategy.nextPrice`, while the injected `RandomWalkStrategy` or `TrendStrategy` supplies the implementation. Entity `equals`, `hashCode`, and `toString` override methods inherited from Object.

12. **Where is method overloading used?** `TradeService.calculateQuantity(amount, price)` handles the arithmetic. `calculateQuantity(amount, stock)` extracts the price and delegates. Java resolves the overload from argument types; overriding is selected by the runtime object's class.

13. **Why constructor injection?** A class lists its required dependencies in its constructor. They can be final, the object cannot exist half-configured, and tests can supply mocks without starting Spring. Constructor chaining with `this(...)` reuses initialization; `super(...)` passes exception messages to a superclass.

14. **How does encapsulation help the wallet?** The balance is private and business methods such as `debit` and `credit` validate the operation. A generic public balance setter would allow unrelated code to bypass those rules.

15. **Why unchecked exceptions for business rules?** The custom `SimulatorException` extends RuntimeException, so Spring's default transaction behavior rolls back these failures. Checked exceptions require callers to catch or declare them and normally need explicit rollback configuration. The checked exception example exists to explain this difference.

16. **How do you prevent users trading another person's money?** A valid token identifies the caller, and every user-scoped endpoint compares the requested user ID to the authenticated principal. Authentication alone is insufficient. Passwords use BCrypt, and neither password hashes nor token secrets are returned in public DTOs.

17. **Why six tables, and is the schema normalized?** Users, wallets, stocks, current holdings, immutable transactions, and price history have different responsibilities. Separating them avoids repeating mutable names or emails in every fact. Average holding cost and current price are intentional summaries maintained through transaction rules.

18. **Why those indexes?** `(stock_id, recorded_at)` matches filtering one stock and ordering or ranging by time. `(user_id, created_at)` matches recent user transactions. The unique `(user_id, stock_id)` both enforces one position and supports position lookup. EXPLAIN should confirm the optimizer's actual choice on realistic data.

19. **What is the N+1 problem?** Loading N holdings and then lazily loading each stock can issue one query plus N more. A fetch join, entity graph, or DTO projection retrieves the required relationships together. I would inspect SQL and measure before adding fetch plans everywhere.

20. **How is average acquisition price calculated?** Multiply the previous quantity by its average, add the purchased quantity times its trade price, divide by the total quantity, and round explicitly. A sell keeps the remaining average unchanged; it is not a new purchase.

21. **What does the leaderboard's RANK() do?** It orders users by capital-adjusted profit percentage and assigns equal rank to a tie. If two users rank first, the next ranks third. Deposits increase the baseline, so depositing cash does not create investment profit.

22. **What does LAG() demonstrate?** It reads the preceding price in each stock's time-ordered history without a self-join. Subtracting that value gives the step change. The first row has no predecessor and therefore returns NULL.

23. **How is Java platform independent, and what is the JVM's role?** `javac` compiles Java source to bytecode. A compatible JVM on each operating system loads, verifies, and executes that bytecode, often compiling hot code with its JIT. The JDK includes development tools; the JVM executes the code. Native libraries, OS paths, and external services can still introduce platform dependencies.

24. **How would you scale this?** Keep API instances stateless, share a managed MySQL database, use migrations, tune bounded connection pools, and cache public stock reads where measurements justify it. Elect one scheduler or use a job system so multiple instances do not independently tick prices. Add idempotency keys before automatically retrying writes.

25. **What would you improve before handling real money?** Real money is outside this simulator's scope. A financial product would require an audited ledger, immutable event trails, idempotency, stronger reconciliation, provider integration, operational monitoring, recovery procedures, security review, and regulatory work. For this project I would first add versioned migrations, richer accessibility coverage, rate limiting, token revocation, and more load tests.

## How to demonstrate the project

Run the frontend, register a new account, and show its ₹1,000 wallet. Buy ₹100 of a stock, explain the four-decimal share quantity, inspect the holding and BUY record, then sell part of it. Deposit virtual cash and show that the leaderboard does not count that deposit as a gain. Try an amount below ₹10 and an excessive sale, then show the consistent error response. Use another account's ID to demonstrate the authorization rejection. Finally, run the service and database concurrency tests and explain why one stale writer must roll back.

Use [the SQL examples](backend/src/main/resources/sql/interview_queries.sql) to explain a join, a grouped aggregate, `RANK`, `LAG`, and `EXPLAIN`. Use [the architecture notes](docs/ARCHITECTURE.md) to connect these examples to the HTTP API.
