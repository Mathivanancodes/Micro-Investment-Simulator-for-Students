package com.microinvestment.simulator.service;

import com.microinvestment.simulator.dto.*;
import com.microinvestment.simulator.entity.Stock;
import com.microinvestment.simulator.exception.ResourceNotFoundException;
import com.microinvestment.simulator.repository.*;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StockService {
    private final StockRepository stocks;
    private final PriceHistoryRepository history;

    public StockService(StockRepository stocks, PriceHistoryRepository history) {
        this.stocks = stocks;
        this.history = history;
    }

    public List<StockResponse> all() {
        return stocks.findAll(Sort.by("symbol")).stream().map(this::response).toList();
    }

    public StockResponse get(Long id) {
        return response(requireStock(id));
    }

    public List<PriceHistoryResponse> history(Long id, int days) {
        requireStock(id);
        return history.findByStockIdAndRecordedAtAfterOrderByRecordedAtAsc(id, LocalDateTime.now().minusDays(days))
                .stream().map(point -> new PriceHistoryResponse(point.getId(), id, point.getPrice(), point.getRecordedAt())).toList();
    }

    private Stock requireStock(Long id) {
        return stocks.findById(id).orElseThrow(() -> new ResourceNotFoundException("Stock not found."));
    }

    private StockResponse response(Stock stock) {
        return new StockResponse(stock.getId(), stock.getSymbol(), stock.getName(), stock.getCurrentPrice());
    }
}
