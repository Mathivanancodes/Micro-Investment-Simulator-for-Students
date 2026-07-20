package com.microinvestment.simulator.repository;

import com.microinvestment.simulator.model.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {
    List<PriceHistory> findByStockSymbolOrderByTimestampDesc(String stockSymbol);
    List<PriceHistory> findByStockSymbolAndTimestampAfterOrderByTimestampAsc(String stockSymbol, LocalDateTime timestamp);
}
