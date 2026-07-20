package com.microinvestment.simulator.service;

import com.microinvestment.simulator.model.PriceHistory;
import com.microinvestment.simulator.model.Stock;
import com.microinvestment.simulator.repository.PriceHistoryRepository;
import com.microinvestment.simulator.repository.StockRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class StockSeeder implements CommandLineRunner {

    private final StockRepository stockRepository;
    private final PriceHistoryRepository priceHistoryRepository;

    @Autowired
    public StockSeeder(StockRepository stockRepository, PriceHistoryRepository priceHistoryRepository) {
        this.stockRepository = stockRepository;
        this.priceHistoryRepository = priceHistoryRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        seedStocks();
    }

    private void seedStocks() {
        if (stockRepository.count() > 0) {
            log.info("Stocks database already seeded. Skipping initial seeding.");
            return;
        }

        log.info("Starting stock database seeding...");
        List<StockSeedData> seedList = getStockSeedList();

        for (StockSeedData data : seedList) {
            // Save Stock
            Stock stock = Stock.builder()
                    .symbol(data.symbol)
                    .name(data.name)
                    .currentPrice(data.initialPrice)
                    .build();
            Stock savedStock = stockRepository.save(stock);
            log.info("Seeded stock: {} ({})", savedStock.getName(), savedStock.getSymbol());

            // Seed historical data points for the last 5 days to populate charts nicely
            seedStockHistory(data.symbol, data.initialPrice);
        }
        log.info("Stock database seeding completed successfully.");
    }

    private void seedStockHistory(String symbol, BigDecimal currentPrice) {
        LocalDateTime now = LocalDateTime.now();
        List<PriceHistory> historyList = new ArrayList<>();

        // Generate 5 days of history
        double[] factors = {0.95, 0.98, 1.02, 0.99, 1.00};
        for (int i = 0; i < factors.length; i++) {
            BigDecimal historicalPrice = currentPrice.multiply(BigDecimal.valueOf(factors[i]))
                    .setScale(2, RoundingMode.HALF_UP);
            
            PriceHistory history = PriceHistory.builder()
                    .stockSymbol(symbol)
                    .price(historicalPrice)
                    // Subtract i days from now, but keep them in chronological order
                    .timestamp(now.minusDays(factors.length - 1 - i))
                    .build();
            historyList.add(history);
        }
        priceHistoryRepository.saveAll(historyList);
        log.debug("Seeded 5 historical price records for {}", symbol);
    }

    private List<StockSeedData> getStockSeedList() {
        List<StockSeedData> list = new ArrayList<>();
        // Tech
        list.add(new StockSeedData("AAPL", "Apple Inc.", new BigDecimal("185.00")));
        list.add(new StockSeedData("MSFT", "Microsoft Corporation", new BigDecimal("420.00")));
        list.add(new StockSeedData("GOOGL", "Alphabet Inc.", new BigDecimal("175.00")));
        list.add(new StockSeedData("NVDA", "NVIDIA Corporation", new BigDecimal("120.00")));
        // Consumer/Retail/Auto
        list.add(new StockSeedData("AMZN", "Amazon.com, Inc.", new BigDecimal("180.00")));
        list.add(new StockSeedData("TSLA", "Tesla, Inc.", new BigDecimal("170.00")));
        list.add(new StockSeedData("DIS", "The Walt Disney Company", new BigDecimal("100.00")));
        list.add(new StockSeedData("WMT", "Walmart Inc.", new BigDecimal("65.00")));
        // Finance/Payments
        list.add(new StockSeedData("JPM", "JPMorgan Chase & Co.", new BigDecimal("195.00")));
        list.add(new StockSeedData("BAC", "Bank of America Corporation", new BigDecimal("38.00")));
        list.add(new StockSeedData("V", "Visa Inc.", new BigDecimal("270.00")));
        list.add(new StockSeedData("KO", "The Coca-Cola Company", new BigDecimal("62.00")));
        return list;
    }

    private static class StockSeedData {
        String symbol;
        String name;
        BigDecimal initialPrice;

        StockSeedData(String symbol, String name, BigDecimal initialPrice) {
            this.symbol = symbol;
            this.name = name;
            this.initialPrice = initialPrice;
        }
    }
}
