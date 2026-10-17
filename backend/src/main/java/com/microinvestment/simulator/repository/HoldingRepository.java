package com.microinvestment.simulator.repository;

import com.microinvestment.simulator.entity.Holding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface HoldingRepository extends JpaRepository<Holding, Long> {
    // INTERVIEW: JOIN FETCH retrieves each stock with its holding in one query, avoiding the N+1 problem.
    @Query("select h from Holding h join fetch h.stock where h.user.id = :userId order by h.stock.symbol")
    List<Holding> findByUserId(@Param("userId") Long userId);

    Optional<Holding> findByUserIdAndStockId(Long userId, Long stockId);

    // INTERVIEW: An aggregate query lets the database calculate value without transferring every position.
    @Query("select coalesce(sum(round(h.quantity * h.stock.currentPrice, 2)), 0) from Holding h where h.user.id = :userId")
    BigDecimal findPortfolioValue(@Param("userId") Long userId);

    @Query("select h.user.id from Holding h where h.quantity > 0 group by h.user.id having count(h) > :minimum")
    List<Long> findUserIdsWithMoreThanHoldings(@Param("minimum") long minimum);
}
