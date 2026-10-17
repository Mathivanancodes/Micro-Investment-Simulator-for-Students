package com.microinvestment.simulator.service;

import com.microinvestment.simulator.strategy.PriceStrategy;
import com.microinvestment.simulator.strategy.RandomWalkStrategy;
import com.microinvestment.simulator.strategy.TrendStrategy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PriceStrategyTest {
    @Test
    void randomWalkPricesAlwaysStayWithinTwoPercentAndUseTwoDecimals() {
        PriceStrategy strategy = new RandomWalkStrategy();
        for (int i = 0; i < 1000; i++) {
            BigDecimal price = strategy.nextPrice(new BigDecimal("1000.00"));
            assertThat(price).isBetween(new BigDecimal("980.00"), new BigDecimal("1020.00"));
            assertThat(price.scale()).isEqualTo(2);
        }
    }

    @Test
    void strategyNeverProducesAZeroOrNegativePennyStockPrice() {
        PriceStrategy strategy = new RandomWalkStrategy();
        for (int i = 0; i < 100; i++) {
            assertThat(strategy.nextPrice(new BigDecimal("0.01"))).isPositive();
        }
    }

    @Test
    void trendImplementationCanBeUsedThroughTheSameStrategyContract() {
        PriceStrategy strategy = new TrendStrategy();
        assertThat(strategy.nextPrice(new BigDecimal("100.00"))).isEqualByComparingTo("100.50");
        assertThat(strategy.nextPrice(new BigDecimal("100.50"))).isEqualByComparingTo("101.00");
    }

    @Test
    void bothStrategiesRejectInvalidStartingPrices() {
        for (PriceStrategy strategy : List.of(new RandomWalkStrategy(), new TrendStrategy())) {
            assertThatThrownBy(() -> strategy.nextPrice(null)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> strategy.nextPrice(BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> strategy.nextPrice(BigDecimal.ONE.negate())).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
