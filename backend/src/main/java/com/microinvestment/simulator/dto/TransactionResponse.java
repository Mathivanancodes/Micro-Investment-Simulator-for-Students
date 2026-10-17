package com.microinvestment.simulator.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(Long id, Long stockId, String symbol, String type,
                                  BigDecimal quantity, BigDecimal price, BigDecimal amount,
                                  LocalDateTime createdAt) {
}
