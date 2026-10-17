package com.microinvestment.simulator.scheduler;

import com.microinvestment.simulator.entity.PriceHistory;
import com.microinvestment.simulator.entity.Stock;
import com.microinvestment.simulator.repository.PriceHistoryRepository;
import com.microinvestment.simulator.repository.StockRepository;
import com.microinvestment.simulator.strategy.PriceStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@ConditionalOnProperty(name = "app.prices.enabled", havingValue = "true", matchIfMissing = true)
public class PriceUpdateScheduler {
    private static final Logger log = LoggerFactory.getLogger(PriceUpdateScheduler.class);
    private final StockRepository stocks;
    private final PriceHistoryRepository histories;
    private final PriceStrategy strategy;

    // INTERVIEW: Constructor injection makes required collaborators explicit and supports isolated tests.
    public PriceUpdateScheduler(StockRepository stocks, PriceHistoryRepository histories, PriceStrategy strategy) {
        this.stocks = stocks;
        this.histories = histories;
        this.strategy = strategy;
    }

    @Scheduled(fixedDelayString = "${app.prices.interval-ms:30000}", initialDelayString = "${app.prices.interval-ms:30000}")
    @Transactional
    public void updatePrices() {
        int updated = 0;
        try {
            for (Stock stock : stocks.findAll()) {
                BigDecimal next = strategy.nextPrice(stock.getCurrentPrice());
                stock.updatePrice(next);
                histories.save(new PriceHistory(stock, next));
                updated++;
            }
        } catch (RuntimeException exception) {
            // INTERVIEW: An unchecked exception rolls back both current prices and their history rows.
            log.warn("Price update failed; the batch will roll back", exception);
            throw exception;
        } finally {
            // INTERVIEW: finally runs after success or failure; commit occurs after this proxied method returns.
            log.debug("Price update processed {} stocks; transaction completion follows", updated);
        }
    }
}
