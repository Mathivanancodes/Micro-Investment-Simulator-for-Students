package com.microinvestment.simulator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioSummaryDto {
    private Long userId;
    private BigDecimal walletBalance;
    private BigDecimal holdingsValue;
    private BigDecimal totalValue;
    private List<HoldingSummaryDto> holdings;
    private List<HistoryPoint> historicalValue;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HistoryPoint {
        private String date;
        private BigDecimal value;
    }
}
