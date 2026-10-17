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
public class StockController {
    private final StockService stocks;
    public StockController(StockService stocks) { this.stocks = stocks; }
    @GetMapping("/stocks")
    public List<StockResponse> all() { return stocks.all(); }
    @GetMapping("/stocks/{id}")
    public StockResponse get(@PathVariable @Positive Long id) { return stocks.get(id); }
    @GetMapping("/stocks/{id}/history")
    public List<PriceHistoryResponse> history(@PathVariable @Positive Long id,
            @RequestParam(defaultValue="7") @Min(1) @Max(365) int days) { return stocks.history(id, days); }
}
