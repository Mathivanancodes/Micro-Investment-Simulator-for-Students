package com.microinvestment.simulator.dto;

import java.time.LocalDateTime;

public record ErrorResponse(LocalDateTime timestamp, int status, String error, String message, String path) {
}
