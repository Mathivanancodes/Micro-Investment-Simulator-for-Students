package com.microinvestment.simulator.strategy;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ThreadLocalRandom;

@Component
@ConditionalOnProperty(name = "app.price-strategy", havingValue = "random", matchIfMissing = true)
public class RandomWalkStrategy implements PriceStrategy {
    // INTERVIEW: Overriding implements the strategy contract; the scheduler invokes it polymorphically.
    @Override
    public BigDecimal nextPrice(BigDecimal current) {
        if (current == null || current.signum() <= 0) {
            throw new IllegalArgumentException("Current price must be positive");
        }
        // Integer basis points produce inclusive -2% to +2% without binary floating-point calculations.
        int basisPoints = ThreadLocalRandom.current().nextInt(-200, 201);
        BigDecimal multiplier = BigDecimal.ONE.add(BigDecimal.valueOf(basisPoints, 4));
        return current.multiply(multiplier).setScale(2, RoundingMode.HALF_UP).max(new BigDecimal("0.01"));
    }
}
