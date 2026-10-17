package com.microinvestment.simulator.repository;

import java.math.BigDecimal;

/** A read-only view of database-calculated performance; no managed entities cross the API. */
public interface LeaderboardProjection {
    Long getUserId();
    String getName();
    BigDecimal getTotalValue();
    BigDecimal getInvested();
    BigDecimal getPnl();
    BigDecimal getReturnRate();
    Long getTradesCount();
    Long getRank();
}
