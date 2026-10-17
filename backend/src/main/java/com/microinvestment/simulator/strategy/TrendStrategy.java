package com.microinvestment.simulator.strategy;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** A deterministic +0.50% trend supports reproducible demonstrations. */
@Component
@ConditionalOnProperty(name = "app.price-strategy", havingValue = "trend")
public class TrendStrategy implements PriceStrategy {
    // INTERVIEW: A second override supplies different behavior through the same PriceStrategy interface.
    @Override
    public BigDecimal nextPrice(BigDecimal current) {
        if (current == null || current.signum() <= 0) {
            throw new IllegalArgumentException("Current price must be positive");
        }
        return current.multiply(new BigDecimal("1.0050")).setScale(2, RoundingMode.HALF_UP);
    }
}
