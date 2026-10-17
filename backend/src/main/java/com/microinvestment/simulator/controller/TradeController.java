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
public class TradeController {
    private final TradeService trades;
    private final AccessPolicy access;
    public TradeController(TradeService trades, AccessPolicy access) { this.trades = trades; this.access = access; }
    @PostMapping("/trade/buy")
    public TradeResponse buy(@Valid @RequestBody BuyRequest request) {
        access.requireOwner(request.userId()); return trades.buyStock(request.userId(), request.stockId(), request.amount());
    }
    @PostMapping("/trade/sell")
    public TradeResponse sell(@Valid @RequestBody SellRequest request) {
        access.requireOwner(request.userId()); return trades.sellStock(request.userId(), request.stockId(), request.quantity());
    }
}
