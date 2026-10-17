package com.microinvestment.simulator.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record BuyRequest(
        @NotNull(message = "User ID is required") @Positive(message = "User ID must be positive") Long userId,
        @NotNull(message = "Stock ID is required") @Positive(message = "Stock ID must be positive") Long stockId,
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "10.00", message = "Buy amount must be at least 10.00")
        @DecimalMax(value = "500.00", message = "Buy amount must be at most 500.00")
        @Digits(integer = 3, fraction = 2, message = "Amount supports at most 2 decimal places") BigDecimal amount) {
}
