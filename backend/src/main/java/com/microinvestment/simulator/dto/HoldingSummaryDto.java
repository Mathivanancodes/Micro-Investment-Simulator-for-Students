package com.microinvestment.simulator.dto;

import java.math.BigDecimal;

public record HoldingSummaryDto(Long stockId, String symbol, String name, BigDecimal quantity,
                                BigDecimal averagePrice, BigDecimal currentPrice, BigDecimal marketValue,
                                BigDecimal invested, BigDecimal pnl, BigDecimal pnlPercentage) {
}
