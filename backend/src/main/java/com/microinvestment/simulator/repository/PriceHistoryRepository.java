package com.microinvestment.simulator.repository;

import com.microinvestment.simulator.entity.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {
    List<PriceHistory> findByStockIdAndRecordedAtAfterOrderByRecordedAtAsc(Long stockId, LocalDateTime recordedAt);
    boolean existsByStockId(Long stockId);
}
