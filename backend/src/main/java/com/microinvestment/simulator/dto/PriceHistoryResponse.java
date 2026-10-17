package com.microinvestment.simulator.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PriceHistoryResponse(Long id, Long stockId, BigDecimal price, LocalDateTime recordedAt) {
}
