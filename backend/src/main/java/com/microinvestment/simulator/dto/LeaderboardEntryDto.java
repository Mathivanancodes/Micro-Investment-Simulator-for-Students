package com.microinvestment.simulator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaderboardEntryDto {
    private int rank;
    private Long userId;
    private String username;
    private BigDecimal totalValue;
    private BigDecimal returnRate;
    private int tradesCount;
}
