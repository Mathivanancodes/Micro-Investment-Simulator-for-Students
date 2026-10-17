package com.microinvestment.simulator.dto;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioSummaryDto(Long userId, BigDecimal walletBalance, BigDecimal holdingsValue,
                                  BigDecimal totalValue, BigDecimal invested, BigDecimal pnl,
                                  BigDecimal pnlPercentage, List<HoldingSummaryDto> holdings,
                                  List<HistoryPoint> historicalValue) {
    public record HistoryPoint(String date, BigDecimal value) {
    }
}
