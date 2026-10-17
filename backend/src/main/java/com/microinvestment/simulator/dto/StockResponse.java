package com.microinvestment.simulator.dto;

import java.math.BigDecimal;

public record StockResponse(Long id, String symbol, String name, BigDecimal currentPrice) {
}
