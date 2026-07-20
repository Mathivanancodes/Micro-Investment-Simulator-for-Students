package com.microinvestment.simulator.scheduler;

import com.microinvestment.simulator.model.PriceHistory;
import com.microinvestment.simulator.model.Stock;
import com.microinvestment.simulator.repository.PriceHistoryRepository;
import com.microinvestment.simulator.repository.StockRepository;
import com.microinvestment.simulator.service.MarketDataClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@Slf4j
public class PriceUpdateScheduler {

    private final StockRepository stockRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final MarketDataClient marketDataClient;

    @Value("${market.data.api.key}")
    private String apiKey;

    @Autowired
    public PriceUpdateScheduler(StockRepository stockRepository,
                                PriceHistoryRepository priceHistoryRepository,
                                MarketDataClient marketDataClient) {
        this.stockRepository = stockRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.marketDataClient = marketDataClient;
    }

    /**
     * Scheduled job that updates stock prices every 5 minutes.
     * FixedRate is 300,000 milliseconds (5 minutes).
     */
    @Scheduled(fixedRate = 300000, initialDelay = 60000) // Delay first run by 1 minute to allow startup seeder to settle
    public void updatePrices() {
        log.info("Scheduled price update job starting...");
        List<Stock> stocks = stockRepository.findAll();

        if (stocks.isEmpty()) {
            log.warn("No stocks found in the database. Scheduled update skipped.");
            return;
        }

        boolean isMock = apiKey == null || 
                         apiKey.trim().isEmpty() || 
                         "YOUR_TWELVE_DATA_API_KEY_HERE".equalsIgnoreCase(apiKey.trim());

        for (int i = 0; i < stocks.size(); i++) {
            Stock stock = stocks.get(i);
            try {
                log.info("Updating price for stock: {}", stock.getSymbol());
                BigDecimal newPrice = marketDataClient.fetchCurrentPrice(stock.getSymbol());

                // Update Stock table
                stock.setCurrentPrice(newPrice);
                stockRepository.save(stock);

                // Insert into PriceHistory table
                PriceHistory history = PriceHistory.builder()
                        .stockSymbol(stock.getSymbol())
                        .price(newPrice)
                        .build();
                priceHistoryRepository.save(history);
                
                log.info("Successfully updated {} price to ${}", stock.getSymbol(), newPrice);

                // Apply delay if NOT in mock mode and NOT the last item to prevent Twelve Data rate limit errors (max 8 requests/min)
                if (!isMock && i < (stocks.size() - 1)) {
                    log.info("Sleeping for 8 seconds to respect free API rate limits...");
                    Thread.sleep(8000);
                }

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.warn("Scheduled price update thread interrupted.", ie);
                break; // Exit loop if thread is interrupted
            } catch (Exception e) {
                log.error("Error updating price for stock {}: {}", stock.getSymbol(), e.getMessage());
                // Continue to the next stock since we wrap each iteration in try-catch
            }
        }

        log.info("Scheduled price update job completed.");
    }
}
