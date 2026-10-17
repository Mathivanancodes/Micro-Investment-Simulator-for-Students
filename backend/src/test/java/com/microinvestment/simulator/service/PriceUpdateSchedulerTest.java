package com.microinvestment.simulator.service;

import com.microinvestment.simulator.entity.PriceHistory;
import com.microinvestment.simulator.entity.Stock;
import com.microinvestment.simulator.repository.PriceHistoryRepository;
import com.microinvestment.simulator.repository.StockRepository;
import com.microinvestment.simulator.scheduler.PriceUpdateScheduler;
import com.microinvestment.simulator.strategy.PriceStrategy;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PriceUpdateSchedulerTest {
    @Test
    void scheduledRefreshUpdatesEveryStockAndPersistsItsNewHistoryPoint() {
        StockRepository stocks = mock(StockRepository.class);
        PriceHistoryRepository histories = mock(PriceHistoryRepository.class);
        Stock first = new Stock("ONE", "One Company", new BigDecimal("100.00"));
        Stock second = new Stock("TWO", "Two Company", new BigDecimal("200.00"));
        when(stocks.findAll()).thenReturn(List.of(first, second));
        PriceStrategy strategy = current -> current.add(new BigDecimal("1.00"));

        new PriceUpdateScheduler(stocks, histories, strategy).updatePrices();

        assertThat(first.getCurrentPrice()).isEqualByComparingTo("101.00");
        assertThat(second.getCurrentPrice()).isEqualByComparingTo("201.00");
        ArgumentCaptor<PriceHistory> points = ArgumentCaptor.forClass(PriceHistory.class);
        verify(histories, times(2)).save(points.capture());
        assertThat(points.getAllValues()).extracting(PriceHistory::getPrice)
                .containsExactly(new BigDecimal("101.00"), new BigDecimal("201.00"));
        assertThat(points.getAllValues()).extracting(PriceHistory::getStock).containsExactly(first, second);
    }

    @Test
    void refreshFailurePropagatesSoTheTransactionCanRollBack() {
        StockRepository stocks = mock(StockRepository.class);
        PriceHistoryRepository histories = mock(PriceHistoryRepository.class);
        when(stocks.findAll()).thenReturn(List.of(new Stock("ONE", "One Company", new BigDecimal("100.00"))));
        PriceStrategy failed = current -> { throw new IllegalStateException("strategy unavailable"); };

        assertThatThrownBy(() -> new PriceUpdateScheduler(stocks, histories, failed).updatePrices())
                .isInstanceOf(IllegalStateException.class).hasMessage("strategy unavailable");
    }
}
