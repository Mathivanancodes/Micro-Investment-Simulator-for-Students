package com.microinvestment.simulator.repository;

import com.microinvestment.simulator.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findBySymbol(String symbol);
    boolean existsBySymbol(String symbol);
}
