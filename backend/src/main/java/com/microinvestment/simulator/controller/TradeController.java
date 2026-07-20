package com.microinvestment.simulator.controller;

import com.microinvestment.simulator.dto.TradeRequest;
import com.microinvestment.simulator.dto.TradeResponse;
import com.microinvestment.simulator.service.TradeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

@RestController
@RequestMapping("/api/trade")
@CrossOrigin(origins = "*")
public class TradeController {

    private final TradeService tradeService;

    @Autowired
    public TradeController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    /**
     * POST /api/trade/buy
     * Executes a stock buy transaction.
     */
    @PostMapping("/buy")
    public ResponseEntity<TradeResponse> buyStock(@Valid @RequestBody TradeRequest request) {
        TradeResponse response = tradeService.buyStock(
                request.getUserId(),
                request.getSymbol(),
                request.getQuantity()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/trade/sell
     * Executes a stock sell transaction.
     */
    @PostMapping("/sell")
    public ResponseEntity<TradeResponse> sellStock(@Valid @RequestBody TradeRequest request) {
        TradeResponse response = tradeService.sellStock(
                request.getUserId(),
                request.getSymbol(),
                request.getQuantity()
        );
        return ResponseEntity.ok(response);
    }
}
