package com.microinvestment.simulator.repository;

import com.microinvestment.simulator.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    @EntityGraph(attributePaths = "stock")
    Page<Transaction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = "stock")
    List<Transaction> findByUserIdOrderByCreatedAtAsc(Long userId);
}
