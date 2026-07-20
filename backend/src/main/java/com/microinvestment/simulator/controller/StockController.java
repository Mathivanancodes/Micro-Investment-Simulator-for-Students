package com.microinvestment.simulator.controller;

import com.microinvestment.simulator.model.PriceHistory;
import com.microinvestment.simulator.model.Stock;
import com.microinvestment.simulator.repository.PriceHistoryRepository;
import com.microinvestment.simulator.repository.StockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/stocks")
@CrossOrigin(origins = "*")
public class StockController {

    private final StockRepository stockRepository;
    private final PriceHistoryRepository priceHistoryRepository;

    @Autowired
    public StockController(StockRepository stockRepository, PriceHistoryRepository priceHistoryRepository) {
        this.stockRepository = stockRepository;
        this.priceHistoryRepository = priceHistoryRepository;
    }

    /**
     * GET /api/stocks
     * List all stocks with their current prices.
     */
    @GetMapping
    public ResponseEntity<List<Stock>> getAllStocks() {
        List<Stock> stocks = stockRepository.findAll();
        return ResponseEntity.ok(stocks);
    }

    /**
     * GET /api/stocks/{symbol}/history
     * Returns the price history of a specific stock for charting.
     * Optional 'days' query parameter to fetch the last N days (defaults to 7 days).
     */
    @GetMapping("/{symbol}/history")
    public ResponseEntity<?> getStockHistory(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "7") int days) {
        
        String upperSymbol = symbol.toUpperCase();
        
        // Verify that the stock exists in our system
        Optional<Stock> stockOpt = stockRepository.findBySymbol(upperSymbol);
        if (stockOpt.isEmpty()) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Stock symbol not found: " + upperSymbol);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }

        // Calculate starting date/time for retrieval
        LocalDateTime startingTime = LocalDateTime.now().minusDays(days);
        
        // Fetch history ordered chronologically (oldest to newest) for charts
        List<PriceHistory> history = priceHistoryRepository
                .findByStockSymbolAndTimestampAfterOrderByTimestampAsc(upperSymbol, startingTime);
        
        return ResponseEntity.ok(history);
    }
}
