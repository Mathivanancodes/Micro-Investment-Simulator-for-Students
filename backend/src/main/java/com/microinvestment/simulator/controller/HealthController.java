package com.microinvestment.simulator.controller;

import com.microinvestment.simulator.dto.ErrorResponse;
import com.microinvestment.simulator.dto.HealthResponse;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {
    private final JdbcTemplate jdbc;

    public HealthController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        try {
            Integer result = jdbc.queryForObject("SELECT 1", Integer.class);
            if (!Integer.valueOf(1).equals(result)) {
                throw new IllegalStateException("Unexpected database health result");
            }
            return ResponseEntity.ok(new HealthResponse("UP", "CONNECTED", "Simulator is running."));
        } catch (RuntimeException exception) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ErrorResponse(
                    LocalDateTime.now(), 503, "Service Unavailable", "Database connection is unavailable.", "/api/health"));
        }
    }
}
