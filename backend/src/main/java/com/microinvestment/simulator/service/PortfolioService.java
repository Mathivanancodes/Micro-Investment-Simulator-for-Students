package com.microinvestment.simulator.service;

import com.microinvestment.simulator.dto.*;
import com.microinvestment.simulator.entity.*;
import com.microinvestment.simulator.exception.ResourceNotFoundException;
import com.microinvestment.simulator.repository.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PortfolioService {
    private final UserRepository users;
    private final WalletRepository wallets;
    private final HoldingRepository holdings;
    private final TransactionRepository transactions;
    private final PriceHistoryRepository history;

    public PortfolioService(UserRepository users, WalletRepository wallets, HoldingRepository holdings,
                            TransactionRepository transactions, PriceHistoryRepository history) {
        this.users = users;
        this.wallets = wallets;
        this.holdings = holdings;
        this.transactions = transactions;
        this.history = history;
    }

    public PortfolioSummaryDto getPortfolioSummary(Long userId) {
        User user = users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found."));
        Wallet wallet = wallets.findByUserId(userId).orElseThrow(() -> new ResourceNotFoundException("Wallet not found."));
        List<HoldingSummaryDto> positions = holdings.findByUserId(userId).stream().map(this::holding).toList();
        BigDecimal value = positions.stream().map(HoldingSummaryDto::marketValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal invested = positions.stream().map(HoldingSummaryDto::invested).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pnl = value.subtract(invested);
        return new PortfolioSummaryDto(userId, wallet.getBalance(), value, wallet.getBalance().add(value),
                invested, pnl, percentage(pnl, invested), positions, historicalValue(user));
    }

    private HoldingSummaryDto holding(Holding holding) {
        Stock stock = holding.getStock();
        BigDecimal value = holding.getQuantity().multiply(stock.getCurrentPrice()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal invested = holding.getQuantity().multiply(holding.getAvgBuyPrice()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal pnl = value.subtract(invested);
        return new HoldingSummaryDto(stock.getId(), stock.getSymbol(), stock.getName(), holding.getQuantity(),
                holding.getAvgBuyPrice(), stock.getCurrentPrice(), value, invested, pnl, percentage(pnl, invested));
    }

    private BigDecimal percentage(BigDecimal pnl, BigDecimal invested) {
        return invested.signum() == 0 ? BigDecimal.ZERO.setScale(2)
                : pnl.multiply(new BigDecimal("100")).divide(invested, 2, RoundingMode.HALF_UP);
    }

    /** Replays actual dated cash flows and share quantities, so new purchases do not appear in earlier days. */
    private List<PortfolioSummaryDto.HistoryPoint> historicalValue(User user) {
        List<Transaction> ledger = transactions.findByUserIdOrderByCreatedAtAsc(user.getId());
        Map<Long, List<PriceHistory>> prices = new HashMap<>();
        Map<Long, Stock> securities = new HashMap<>();
        for (Transaction tx : ledger) {
            if (tx.getStock() != null) {
                Stock stock = tx.getStock();
                securities.put(stock.getId(), stock);
                prices.computeIfAbsent(stock.getId(), id -> history
                        .findByStockIdAndRecordedAtAfterOrderByRecordedAtAsc(id, LocalDateTime.now().minusDays(8)));
            }
        }
        List<PortfolioSummaryDto.HistoryPoint> points = new ArrayList<>();
        for (int offset = 6; offset >= 0; offset--) {
            LocalDate date = LocalDate.now().minusDays(offset);
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            BigDecimal cash = user.getCreatedAt().isBefore(end) ? new BigDecimal("1000.00") : BigDecimal.ZERO;
            Map<Long, BigDecimal> quantities = new HashMap<>();
            for (Transaction tx : ledger) {
                if (!tx.getCreatedAt().isBefore(end)) continue;
                if (tx.getType() == TransactionType.BUY) {
                    cash = cash.subtract(tx.getAmount());
                    quantities.merge(tx.getStock().getId(), tx.getQuantity(), BigDecimal::add);
                } else if (tx.getType() == TransactionType.SELL) {
                    cash = cash.add(tx.getAmount());
                    quantities.merge(tx.getStock().getId(), tx.getQuantity().negate(), BigDecimal::add);
                } else {
                    cash = cash.add(tx.getAmount());
                }
            }
            BigDecimal value = cash;
            for (var position : quantities.entrySet()) {
                BigDecimal price = securities.get(position.getKey()).getCurrentPrice();
                for (PriceHistory point : prices.get(position.getKey())) {
                    if (point.getRecordedAt().isBefore(end)) price = point.getPrice();
                }
                value = value.add(position.getValue().multiply(price));
            }
            points.add(new PortfolioSummaryDto.HistoryPoint(date.toString(), value.setScale(2, RoundingMode.HALF_UP)));
        }
        return points;
    }
}
