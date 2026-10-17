package com.microinvestment.simulator.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record SellRequest(
        @NotNull(message = "User ID is required") @Positive(message = "User ID must be positive") Long userId,
        @NotNull(message = "Stock ID is required") @Positive(message = "Stock ID must be positive") Long stockId,
        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.0001", message = "Quantity must be at least 0.0001")
        @Digits(integer = 11, fraction = 4, message = "Quantity supports at most 11 integer digits and 4 decimal places") BigDecimal quantity) {
}
