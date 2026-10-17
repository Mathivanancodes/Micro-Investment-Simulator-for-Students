package com.microinvestment.simulator.controller;

import com.microinvestment.simulator.config.AccessPolicy;
import com.microinvestment.simulator.dto.*;
import com.microinvestment.simulator.exception.SimulationExportException;
import com.microinvestment.simulator.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Validated
public class PortfolioController {
    private final PortfolioService portfolio;
    private final TransactionService transactions;
    private final AccessPolicy access;
    public PortfolioController(PortfolioService portfolio, TransactionService transactions, AccessPolicy access) {
        this.portfolio = portfolio; this.transactions = transactions; this.access = access;
    }
    @GetMapping("/portfolio/{userId}")
    public PortfolioSummaryDto portfolio(@PathVariable @Positive Long userId) {
        access.requireOwner(userId); return portfolio.getPortfolioSummary(userId);
    }
    @GetMapping("/transactions/{userId}")
    public PageResponse<TransactionResponse> transactions(@PathVariable @Positive Long userId,
            @RequestParam(defaultValue="0") @Min(0) int page, @RequestParam(defaultValue="10") @Min(1) @Max(100) int size) {
        access.requireOwner(userId); return transactions.page(userId, page, size);
    }
    @GetMapping(value="/transactions/{userId}/export", produces="text/csv")
    public ResponseEntity<String> export(@PathVariable @Positive Long userId) throws SimulationExportException {
        access.requireOwner(userId);
        return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=transactions.csv")
                .body(transactions.exportCsv(userId));
    }
}
