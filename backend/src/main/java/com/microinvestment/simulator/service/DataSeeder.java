package com.microinvestment.simulator.service;

import com.microinvestment.simulator.entity.PriceHistory;
import com.microinvestment.simulator.entity.Stock;
import com.microinvestment.simulator.entity.User;
import com.microinvestment.simulator.entity.Wallet;
import com.microinvestment.simulator.repository.PriceHistoryRepository;
import com.microinvestment.simulator.repository.StockRepository;
import com.microinvestment.simulator.repository.UserRepository;
import com.microinvestment.simulator.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/** Optional, repeatable development fixtures. Disabled unless explicitly enabled (normally through the dev profile). */
@Component
@ConditionalOnProperty(name = "app.seed-demo", havingValue = "true")
public class DataSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private final StockRepository stocks;
    private final PriceHistoryRepository histories;
    private final UserRepository users;
    private final WalletRepository wallets;
    private final PasswordEncoder passwordEncoder;
    private final String demoPassword;

    public DataSeeder(StockRepository stocks, PriceHistoryRepository histories, UserRepository users,
                      WalletRepository wallets, PasswordEncoder passwordEncoder,
                      @Value("${app.demo-password:}") String demoPassword) {
        this.stocks = stocks;
        this.histories = histories;
        this.users = users;
        this.wallets = wallets;
        this.passwordEncoder = passwordEncoder;
        this.demoPassword = demoPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        if (demoPassword.isBlank() || demoPassword.length() < 12
                || demoPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("DEMO_PASSWORD must contain at least 12 characters and at most 72 UTF-8 bytes when demo seeding is enabled");
        }

        for (StockSeed seed : stockSeeds()) {
            Stock stock = stocks.findBySymbol(seed.symbol()).orElseGet(() ->
                    stocks.save(new Stock(seed.symbol(), seed.name(), new BigDecimal(seed.price()))));
            if (!histories.existsByStockId(stock.getId())) {
                seedHistory(stock);
            }
        }
        seedUser("Asha Student", "student1@example.test");
        seedUser("Ravi Student", "student2@example.test");
        log.info("Demo fixtures are ready: 8 virtual stocks and 2 demo users; passwords are supplied through configuration");
    }

    private void seedUser(String name, String email) {
        User user = users.findByEmail(email).orElseGet(() ->
                users.save(new User(name, email, passwordEncoder.encode(demoPassword))));
        if (wallets.findByUserId(user.getId()).isEmpty()) {
            wallets.save(new Wallet(user));
        }
    }

    private void seedHistory(Stock stock) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        List<String> multipliers = List.of("0.97", "0.98", "1.01", "0.99", "1.02", "0.98", "0.99", "1.00");
        for (int index = 0; index < multipliers.size(); index++) {
            BigDecimal price = stock.getCurrentPrice().multiply(new BigDecimal(multipliers.get(index)))
                    .setScale(2, RoundingMode.HALF_UP);
            histories.save(new PriceHistory(stock, price, now.minusDays(multipliers.size() - 1L - index)));
        }
    }

    private static List<StockSeed> stockSeeds() {
        return List.of(
                new StockSeed("AAPL", "Apple Inc.", "185.00"),
                new StockSeed("MSFT", "Microsoft Corporation", "420.00"),
                new StockSeed("GOOGL", "Alphabet Inc.", "175.00"),
                new StockSeed("NVDA", "NVIDIA Corporation", "120.00"),
                new StockSeed("AMZN", "Amazon.com Inc.", "180.00"),
                new StockSeed("TSLA", "Tesla Inc.", "170.00"),
                new StockSeed("DIS", "The Walt Disney Company", "100.00"),
                new StockSeed("WMT", "Walmart Inc.", "65.00"));
    }

    private record StockSeed(String symbol, String name, String price) { }
}
