package com.microinvestment.simulator.dto;

import java.math.BigDecimal;

public record LeaderboardEntryDto(Long userId, String name, BigDecimal totalValue, BigDecimal invested,
                                  BigDecimal pnl, BigDecimal returnRate, long tradesCount, long rank) {
}
