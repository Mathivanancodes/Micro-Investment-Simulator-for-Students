package com.microinvestment.simulator.controller;

import com.microinvestment.simulator.dto.PortfolioSummaryDto;
import com.microinvestment.simulator.model.Transaction;
import com.microinvestment.simulator.service.PortfolioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Allow React dev server calls directly
public class PortfolioController {

    private final PortfolioService portfolioService;

    @Autowired
    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    /**
     * GET /api/portfolio/{userId}
     * Returns the complete portfolio aggregation summary for a user.
     */
    @GetMapping("/portfolio/{userId}")
    public ResponseEntity<PortfolioSummaryDto> getPortfolioSummary(@PathVariable Long userId) {
        PortfolioSummaryDto summary = portfolioService.getPortfolioSummary(userId);
        return ResponseEntity.ok(summary);
    }

    /**
     * GET /api/transactions/{userId}
     * Returns the user's chronological transaction log history.
     */
    @GetMapping("/transactions/{userId}")
    public ResponseEntity<List<Transaction>> getTransactionHistory(@PathVariable Long userId) {
        List<Transaction> transactions = portfolioService.getTransactionHistory(userId);
        return ResponseEntity.ok(transactions);
    }
}
