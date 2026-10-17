package com.microinvestment.simulator.repository;

import com.microinvestment.simulator.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    // INTERVIEW: A subquery compares each wallet against the aggregate average without loading every wallet into Java.
    @Query("select u from User u join Wallet w on w.user = u where w.balance > (select avg(w2.balance) from Wallet w2)")
    List<User> findUsersWithAboveAverageWalletBalance();

    // INTERVIEW: Pre-aggregate one-to-many relations before LEFT JOIN to prevent multiplied sums; RANK preserves ties.
    @Query(value = """
            WITH positions AS (
                SELECT h.user_id, SUM(ROUND(h.quantity * s.current_price, 2)) AS market_value,
                       SUM(ROUND(h.quantity * h.avg_buy_price, 2)) AS invested
                FROM holdings h INNER JOIN stocks s ON s.id = h.stock_id
                GROUP BY h.user_id
            ), activity AS (
                SELECT user_id,
                       SUM(CASE WHEN type = 'DEPOSIT' THEN amount ELSE 0 END) AS deposits,
                       SUM(CASE WHEN type IN ('BUY', 'SELL') THEN 1 ELSE 0 END) AS trades_count
                FROM transactions GROUP BY user_id
            ), performance AS (
                SELECT u.id AS userId, u.name AS name,
                       w.balance + COALESCE(p.market_value, 0) AS totalValue,
                       COALESCE(p.invested, 0) AS invested,
                       w.balance + COALESCE(p.market_value, 0) - 1000 - COALESCE(a.deposits, 0) AS pnl,
                       (w.balance + COALESCE(p.market_value, 0) - 1000 - COALESCE(a.deposits, 0))
                           * 100 / (1000 + COALESCE(a.deposits, 0)) AS returnRate,
                       COALESCE(a.trades_count, 0) AS tradesCount
                FROM users u INNER JOIN wallets w ON w.user_id = u.id
                LEFT JOIN positions p ON p.user_id = u.id
                LEFT JOIN activity a ON a.user_id = u.id
            )
            SELECT userId, name, totalValue, invested, pnl, returnRate, tradesCount,
                   RANK() OVER (ORDER BY returnRate DESC) AS `rank`
            FROM performance ORDER BY `rank`, userId
            """, nativeQuery = true)
    List<LeaderboardProjection> findLeaderboard();
}
