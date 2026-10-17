package com.microinvestment.simulator.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 100, message = "Email must contain at most 100 characters") String email,
        @NotBlank(message = "Password is required")
        @Size(max = 72, message = "Password must contain at most 72 characters") String password) {
}
