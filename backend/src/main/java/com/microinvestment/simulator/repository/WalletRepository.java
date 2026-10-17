package com.microinvestment.simulator.repository;

import com.microinvestment.simulator.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    // Wallet's @Version detects concurrent modifications without holding a database lock during service execution.
    Optional<Wallet> findByUserId(Long userId);
}
