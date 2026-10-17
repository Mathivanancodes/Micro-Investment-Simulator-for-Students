package com.microinvestment.simulator.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DepositRequest(
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Deposit amount must be at least 0.01")
        @Digits(integer = 13, fraction = 2, message = "Amount supports at most 13 integer digits and 2 decimal places") BigDecimal amount) {
}
