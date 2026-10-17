package com.microinvestment.simulator.dto;

import java.math.BigDecimal;

public record TradeResponse(Long transactionId, Long stockId, String symbol, String type,
                            BigDecimal quantity, BigDecimal price, BigDecimal amount,
                            BigDecimal remainingBalance) {
}
