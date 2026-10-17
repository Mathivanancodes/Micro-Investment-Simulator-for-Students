package com.microinvestment.simulator.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microinvestment.simulator.entity.Wallet;
import com.microinvestment.simulator.entity.User;
import com.microinvestment.simulator.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Runs against real MySQL: scripts/check.sh creates and explicitly selects the disposable schema. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "RUN_MYSQL_TESTS", matches = "true")
class SimulatorIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManagerFactory entityManagers;
    @Autowired private UserRepository users;

    @Test
    void registrationCreatesThousandRupeeWalletAndPasswordsAreHashed() throws Exception {
        Account account = register();
        JsonNode wallet = body(perform(get("/api/wallet/" + account.id()), account), 200);
        assertThat(wallet.path("balance").decimalValue()).isEqualByComparingTo("1000.00");
        String passwordHash = jdbc.queryForObject("select password_hash from users where id = ?", String.class, account.id());
        assertThat(passwordHash).isNotEqualTo(account.password()).startsWith("$2");
        JsonNode me = body(perform(get("/api/auth/me"), account), 200);
        assertThat(me.path("userId").longValue()).isEqualTo(account.id());
        assertThat(me.has("passwordHash")).isFalse();

        JsonNode login = body(mvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", account.email(), "password", account.password())))).andReturn(), 200);
        assertThat(login.path("token").asText()).isNotBlank();
        assertThat(login.path("userId").longValue()).isEqualTo(account.id());
    }

    @Test
    void authenticationErrorsAreSafeAndProtectedEndpointsRejectMissingOrBadTokens() throws Exception {
        Account account = register();
        JsonNode invalid = body(mvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", account.email(), "password", "incorrect")))).andReturn(), 401);
        assertError(invalid, 401, "/api/auth/login");
        body(mvc.perform(get("/api/portfolio/" + account.id())).andReturn(), 401);
        body(mvc.perform(get("/api/portfolio/" + account.id()).header("Authorization", "Bearer invalid.token.signature")).andReturn(), 401);

        JsonNode duplicate = body(mvc.perform(post("/api/auth/register").contentType(APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("name", "Another Student", "email", account.email(), "password", account.password())))).andReturn(), 409);
        assertError(duplicate, 409, "/api/auth/register");
    }

    @Test
    void usersCannotReadOrMutateAnotherUsersAssets() throws Exception {
        Account attacker = register();
        Account owner = register();
        long stockId = firstStockId(attacker);
        for (String path : List.of("/api/portfolio/", "/api/transactions/", "/api/wallet/")) {
            JsonNode forbidden = body(perform(get(path + owner.id()), attacker), 403);
            assertError(forbidden, 403, path + owner.id());
        }
        body(perform(get("/api/transactions/" + owner.id() + "/export"), attacker), 403);
        body(perform(post("/api/trade/buy").contentType(APPLICATION_JSON).content(json.writeValueAsString(
                Map.of("userId", owner.id(), "stockId", stockId, "amount", 100))), attacker), 403);
        body(perform(post("/api/trade/sell").contentType(APPLICATION_JSON).content(json.writeValueAsString(
                Map.of("userId", owner.id(), "stockId", stockId, "quantity", 1))), attacker), 403);
        body(perform(post("/api/wallet/" + owner.id() + "/deposit").contentType(APPLICATION_JSON)
                .content("{\"amount\":100}"), attacker), 403);
        assertThat(walletBalance(owner.id())).isEqualByComparingTo("1000.00");
        assertThat(ledgerCount(owner.id())).isZero();
    }

    @Test
    void malformedAndInvalidFinancialRequestsReturn400WithRequestPath() throws Exception {
        Account account = register();
        long stockId = firstStockId(account);
        JsonNode malformed = body(perform(post("/api/trade/buy").contentType(APPLICATION_JSON).content("{broken"), account), 400);
        assertError(malformed, 400, "/api/trade/buy");
        for (String amount : List.of("9.99", "500.01", "100.001", "0", "-10")) {
            JsonNode invalid = body(buy(account, stockId, amount), 400);
            assertError(invalid, 400, "/api/trade/buy");
        }
        for (String quantity : List.of("0.00001", "0", "-1", "100000000000.0000")) {
            JsonNode invalid = body(sell(account, stockId, quantity), 400);
            assertError(invalid, 400, "/api/trade/sell");
        }
        body(perform(post("/api/trade/buy").contentType(APPLICATION_JSON).content("{}"), account), 400);
        body(perform(post("/api/trade/buy").contentType(APPLICATION_JSON).content(json.writeValueAsString(
                Map.of("userId", account.id(), "stockId", new BigDecimal("1.9"), "amount", 100))), account), 400);
        body(perform(post("/api/wallet/" + account.id() + "/deposit").contentType(APPLICATION_JSON)
                .content("{\"amount\":0.001}"), account), 400);
        assertThat(walletBalance(account.id())).isEqualByComparingTo("1000.00");
        assertThat(ledgerCount(account.id())).isZero();
    }

    @Test
    void buyingSellingAndDepositingKeepCashHoldingsAndLedgerConsistent() throws Exception {
        Account account = register();
        long stockId = firstStockId(account);
        JsonNode bought = body(buy(account, stockId, "100.00"), 200);
        BigDecimal quantity = bought.path("quantity").decimalValue();
        assertThat(quantity.scale()).isLessThanOrEqualTo(4);
        assertThat(bought.path("amount").decimalValue()).isEqualByComparingTo("100.00");
        assertThat(bought.path("remainingBalance").decimalValue()).isEqualByComparingTo("900.00");
        assertThat(bought.path("stockId").asLong()).isEqualTo(stockId);
        body(sell(account, stockId, quantity.add(BigDecimal.ONE).toPlainString()), 400);
        assertThat(walletBalance(account.id())).isEqualByComparingTo("900.00");

        JsonNode sold = body(sell(account, stockId, quantity.toPlainString()), 200);
        BigDecimal proceeds = sold.path("amount").decimalValue();
        body(perform(post("/api/wallet/" + account.id() + "/deposit").contentType(APPLICATION_JSON)
                .content("{\"amount\":250.00}"), account), 200);
        assertThat(walletBalance(account.id())).isEqualByComparingTo(new BigDecimal("1150.00").add(proceeds));
        assertThat(jdbc.queryForObject("select count(*) from holdings where user_id = ?", Long.class, account.id())).isZero();
        assertThat(ledgerCount(account.id())).isEqualTo(3);
        assertThat(jdbc.queryForObject("select amount from transactions where user_id = ? and type = 'DEPOSIT'", BigDecimal.class, account.id()))
                .isEqualByComparingTo("250.00");
        JsonNode portfolio = body(perform(get("/api/portfolio/" + account.id()), account), 200);
        assertThat(portfolio.path("holdingsValue").decimalValue()).isEqualByComparingTo("0.00");
        assertThat(portfolio.path("walletBalance").decimalValue()).isEqualByComparingTo(walletBalance(account.id()));
        assertThat(portfolio.path("totalValue").decimalValue()).isEqualByComparingTo(walletBalance(account.id()));
        assertThat(portfolio.path("holdings")).isEmpty();
    }

    @Test
    void buyLimitsAreInclusiveAndInsufficientFundsRollbackEverything() throws Exception {
        Account account = register();
        long stockId = firstStockId(account);
        body(buy(account, stockId, "10.00"), 200);
        body(buy(account, stockId, "500.00"), 200);
        long previous = ledgerCount(account.id());
        JsonNode insufficient = body(buy(account, stockId, "500.00"), 400);
        assertError(insufficient, 400, "/api/trade/buy");
        assertThat(walletBalance(account.id())).isEqualByComparingTo("490.00");
        assertThat(ledgerCount(account.id())).isEqualTo(previous);
    }

    @Test
    void transactionHistoryIsPaginatedSortedAndNeverExposesEntities() throws Exception {
        Account account = register();
        long stockId = firstStockId(account);
        for (int i = 0; i < 3; i++) body(buy(account, stockId, "10.00"), 200);

        JsonNode first = body(perform(get("/api/transactions/" + account.id() + "?page=0&size=2"), account), 200);
        JsonNode second = body(perform(get("/api/transactions/" + account.id() + "?page=1&size=2"), account), 200);
        assertThat(first.path("content").size()).isEqualTo(2);
        assertThat(second.path("content").size()).isEqualTo(1);
        assertThat(first.path("totalElements").asInt()).isEqualTo(3);
        assertThat(first.path("totalPages").asInt()).isEqualTo(2);
        assertThat(first.path("content").get(0).path("id").asLong())
                .isGreaterThan(first.path("content").get(1).path("id").asLong());
        assertThat(first.path("content").get(0).has("user")).isFalse();
        assertThat(first.path("content").get(0).path("createdAt").asText()).isNotBlank();
        body(perform(get("/api/transactions/" + account.id() + "?page=-1"), account), 400);
        body(perform(get("/api/transactions/" + account.id() + "?size=101"), account), 400);
        MvcResult export = perform(get("/api/transactions/" + account.id() + "/export"), account);
        assertThat(export.getResponse().getStatus()).isEqualTo(200);
        assertThat(export.getResponse().getContentType()).startsWith("text/csv");
        assertThat(export.getResponse().getHeader("Content-Disposition")).contains("attachment");
        assertThat(export.getResponse().getContentAsString().lines().count()).isEqualTo(4);
        assertThat(export.getResponse().getContentAsString()).startsWith("id,type,symbol,quantity,price,amount,created_at\n");
    }

    @Test
    void seededStocksHistoryAndLeaderboardAreAvailable() throws Exception {
        Account account = register();
        JsonNode stocks = body(perform(get("/api/stocks"), account), 200);
        assertThat(stocks.isArray()).isTrue();
        assertThat(stocks.size()).isEqualTo(8);
        for (JsonNode stock : stocks) {
            assertThat(stock.path("symbol").asText()).isNotBlank();
            assertThat(stock.path("currentPrice").decimalValue()).isPositive();
        }
        long stockId = stocks.get(0).path("id").asLong();
        JsonNode history = body(perform(get("/api/stocks/" + stockId + "/history"), account), 200);
        assertThat(history.isArray()).isTrue();
        assertThat(history.size()).isGreaterThan(0);
        body(perform(get("/api/stocks/999999999/history"), account), 404);
        JsonNode leaderboard = body(perform(get("/api/leaderboard"), account), 200);
        assertThat(leaderboard.isArray()).isTrue();
        assertThat(leaderboard.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void depositsDoNotCountAsInvestmentProfitAndEqualReturnsReceiveEqualRanks() throws Exception {
        Account initial = register();
        Account depositor = register();
        body(perform(post("/api/wallet/" + depositor.id() + "/deposit").contentType(APPLICATION_JSON)
                .content("{\"amount\":500.00}"), depositor), 200);
        JsonNode leaderboard = body(perform(get("/api/leaderboard"), initial), 200);
        JsonNode initialEntry = null;
        JsonNode depositEntry = null;
        for (JsonNode entry : leaderboard) {
            if (entry.path("userId").asLong() == initial.id()) initialEntry = entry;
            if (entry.path("userId").asLong() == depositor.id()) depositEntry = entry;
        }
        assertThat(initialEntry).isNotNull();
        assertThat(depositEntry).isNotNull();
        assertThat(depositEntry.path("totalValue").decimalValue()).isEqualByComparingTo("1500.00");
        assertThat(depositEntry.path("pnl").decimalValue()).isEqualByComparingTo("0.00");
        assertThat(depositEntry.path("returnRate").decimalValue()).isEqualByComparingTo("0.00");
        assertThat(depositEntry.path("tradesCount").asLong()).isZero();
        assertThat(depositEntry.path("rank").asLong()).isEqualTo(initialEntry.path("rank").asLong());

        List<Long> actualAboveAverage = users.findUsersWithAboveAverageWalletBalance().stream().map(User::getId).sorted().toList();
        List<Long> expectedAboveAverage = jdbc.queryForList("select user_id from wallets where balance > (select avg(balance) from wallets) order by user_id", Long.class);
        assertThat(actualAboveAverage).isEqualTo(expectedAboveAverage);
    }

    @Test
    void optimisticVersionRejectsARealStaleMySqlWalletUpdate() throws Exception {
        Account account = register();
        long walletId = jdbc.queryForObject("select id from wallets where user_id = ?", Long.class, account.id());
        EntityManager first = entityManagers.createEntityManager();
        EntityManager second = entityManagers.createEntityManager();
        try {
            first.getTransaction().begin();
            second.getTransaction().begin();
            Wallet firstRead = first.find(Wallet.class, walletId);
            Wallet staleRead = second.find(Wallet.class, walletId);
            firstRead.debit(new BigDecimal("100.00"));
            first.flush();
            first.getTransaction().commit();
            staleRead.debit(new BigDecimal("200.00"));
            assertThatThrownBy(second::flush).isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("updated or deleted");
            second.getTransaction().rollback();
        } finally {
            if (first.getTransaction().isActive()) first.getTransaction().rollback();
            if (second.getTransaction().isActive()) second.getTransaction().rollback();
            first.close();
            second.close();
        }
        assertThat(walletBalance(account.id())).isEqualByComparingTo("900.00");
    }

    @Test
    void detachedUninitializedJpaProxyHasSafeEqualityHashAndStringRepresentation() throws Exception {
        Account account = register();
        EntityManager loader = entityManagers.createEntityManager();
        User persisted = loader.find(User.class, account.id());
        loader.close();
        EntityManager references = entityManagers.createEntityManager();
        User detachedReference = references.getReference(User.class, account.id());
        references.close();

        assertThat(Hibernate.isInitialized(detachedReference)).isFalse();
        assertThat(persisted.equals(detachedReference)).isTrue();
        assertThat(detachedReference.equals(persisted)).isTrue();
        assertThat(detachedReference.hashCode()).isEqualTo(persisted.hashCode());
        assertThat(detachedReference.toString()).isEqualTo("User{id=" + account.id() + "}");
        assertThat(Hibernate.isInitialized(detachedReference)).isFalse();
    }

    @Test
    void simultaneousHttpOrdersCannotOverspendOrLoseHoldingsAndLedgerEntries() throws Exception {
        Account account = register();
        long stockId = firstStockId(account);
        int requests = 8;
        CountDownLatch ready = new CountDownLatch(requests);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(requests);
        List<Future<MvcResult>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < requests; i++) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Concurrent test start timed out");
                    return buy(account, stockId, "500.00");
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            int successes = 0;
            for (Future<MvcResult> future : futures) {
                MvcResult result = future.get(30, TimeUnit.SECONDS);
                int status = result.getResponse().getStatus();
                assertThat(status).as(result.getResponse().getContentAsString()).isIn(200, 400, 409);
                if (status == 200) successes++;
                if (status == 409) assertError(json.readTree(result.getResponse().getContentAsString()), 409, "/api/trade/buy");
            }
            assertThat(successes).isBetween(1, 2);
            BigDecimal ledgerSpent = jdbc.queryForObject("select coalesce(sum(amount), 0) from transactions where user_id = ? and type = 'BUY'", BigDecimal.class, account.id());
            BigDecimal ledgerQuantity = jdbc.queryForObject("select coalesce(sum(quantity), 0) from transactions where user_id = ? and type = 'BUY'", BigDecimal.class, account.id());
            BigDecimal holdingQuantity = jdbc.queryForObject("select quantity from holdings where user_id = ? and stock_id = ?", BigDecimal.class, account.id(), stockId);
            assertThat(walletBalance(account.id())).isEqualByComparingTo(new BigDecimal("1000.00").subtract(ledgerSpent)).isNotNegative();
            assertThat(holdingQuantity).isEqualByComparingTo(ledgerQuantity);
            assertThat(ledgerCount(account.id())).isEqualTo(successes);
        } finally {
            start.countDown();
            pool.shutdownNow();
        }
    }

    private Account register() throws Exception {
        String email = "student-" + UUID.randomUUID() + "@example.test";
        String password = "Student-Test-123!";
        JsonNode result = body(mvc.perform(post("/api/auth/register").contentType(APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("name", "Test Student", "email", email, "password", password)))).andReturn(), 201);
        return new Account(result.path("userId").asLong(), result.path("token").asText(), email, password);
    }

    private MvcResult buy(Account account, long stockId, String amount) throws Exception {
        return perform(post("/api/trade/buy").contentType(APPLICATION_JSON).content(json.writeValueAsString(
                Map.of("userId", account.id(), "stockId", stockId, "amount", new BigDecimal(amount)))), account);
    }

    private MvcResult sell(Account account, long stockId, String quantity) throws Exception {
        return perform(post("/api/trade/sell").contentType(APPLICATION_JSON).content(json.writeValueAsString(
                Map.of("userId", account.id(), "stockId", stockId, "quantity", new BigDecimal(quantity)))), account);
    }

    private long firstStockId(Account account) throws Exception {
        return body(perform(get("/api/stocks"), account), 200).get(0).path("id").asLong();
    }

    private MvcResult perform(MockHttpServletRequestBuilder request, Account account) throws Exception {
        return mvc.perform(request.header("Authorization", "Bearer " + account.token())).andReturn();
    }

    private JsonNode body(MvcResult result, int expectedStatus) throws Exception {
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(expectedStatus);
        return json.readTree(result.getResponse().getContentAsString());
    }

    private void assertError(JsonNode error, int status, String path) {
        assertThat(error.path("status").asInt()).isEqualTo(status);
        assertThat(error.path("path").asText()).isEqualTo(path);
        assertThat(error.path("timestamp").asText()).isNotBlank();
        assertThat(error.path("message").asText()).isNotBlank();
        assertThat(error.has("trace")).isFalse();
        assertThat(error.has("exception")).isFalse();
    }

    private BigDecimal walletBalance(long userId) {
        return jdbc.queryForObject("select balance from wallets where user_id = ?", BigDecimal.class, userId);
    }

    private long ledgerCount(long userId) {
        return jdbc.queryForObject("select count(*) from transactions where user_id = ?", Long.class, userId);
    }

    private record Account(long id, String token, String email, String password) { }
}
