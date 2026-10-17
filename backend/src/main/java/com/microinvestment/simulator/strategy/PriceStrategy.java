package com.microinvestment.simulator.strategy;

import java.math.BigDecimal;

// INTERVIEW: An interface defines interchangeable behavior; BaseEntity instead shares state and implementation.
public interface PriceStrategy {
    BigDecimal nextPrice(BigDecimal current);
}
