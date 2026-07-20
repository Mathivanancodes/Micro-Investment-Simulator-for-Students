package com.microinvestment.simulator.repository;

import com.microinvestment.simulator.model.Holding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HoldingRepository extends JpaRepository<Holding, Long> {
    List<Holding> findByUserId(Long userId);
    Optional<Holding> findByUserIdAndStockSymbol(Long userId, String stockSymbol);
    void deleteByUserIdAndStockSymbol(Long userId, String stockSymbol);
}
