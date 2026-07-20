package com.microinvestment.simulator.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeResponse {
    private Long transactionId;
    private String symbol;
    private String type;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal totalAmount;
    private BigDecimal remainingBalance;
}
