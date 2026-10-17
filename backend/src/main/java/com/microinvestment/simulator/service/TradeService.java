package com.microinvestment.simulator.service;

import com.microinvestment.simulator.dto.TradeResponse;
import com.microinvestment.simulator.entity.*;
import com.microinvestment.simulator.exception.*;
import com.microinvestment.simulator.repository.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TradeService {
    private final UserRepository users;
    private final WalletRepository wallets;
    private final StockRepository stocks;
    private final HoldingRepository holdings;
    private final TransactionRepository transactions;

    // INTERVIEW: Constructor injection makes required dependencies explicit and easy to mock.
    public TradeService(UserRepository users, WalletRepository wallets, StockRepository stocks,
                        HoldingRepository holdings, TransactionRepository transactions) {
        this.users = users;
        this.wallets = wallets;
        this.stocks = stocks;
        this.holdings = holdings;
        this.transactions = transactions;
    }

    // INTERVIEW: Overloading selects a method by its parameter types at compile time.
    public BigDecimal calculateQuantity(BigDecimal amount, BigDecimal price) {
        if (amount == null || price == null || amount.signum() <= 0 || price.signum() <= 0) {
            throw new BusinessRuleException("Amount and price must be positive.");
        }
        return amount.divide(price, 4, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateQuantity(BigDecimal amount, Stock stock) {
        return calculateQuantity(amount, stock.getCurrentPrice());
    }

    // INTERVIEW: A transaction commits wallet, holding, and audit changes together or rolls all back.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TradeResponse buyStock(Long userId, Long stockId, BigDecimal amount) {
        if (amount == null || amount.compareTo(new BigDecimal("10")) < 0
                || amount.compareTo(new BigDecimal("500")) > 0 || amount.stripTrailingZeros().scale() > 2) {
            throw new BusinessRuleException("Investment amount must be between ₹10 and ₹500 with at most two decimals.");
        }
        User user = requireUser(userId);
        Wallet wallet = requireWallet(userId);
        Stock stock = requireStock(stockId);
        BigDecimal quantity = calculateQuantity(amount, stock);
        if (quantity.signum() == 0) {
            throw new BusinessRuleException("Investment is too small to buy 0.0001 shares at this price.");
        }
        wallet.debit(amount);
        Holding holding = holdings.findByUserIdAndStockId(userId, stockId).orElse(null);
        if (holding == null) {
            holding = new Holding(user, stock, quantity, stock.getCurrentPrice());
        } else {
            holding.add(quantity, stock.getCurrentPrice());
        }
        // INTERVIEW: @Version rejects stale wallet updates; no request retries or partial commits are hidden.
        wallets.saveAndFlush(wallet);
        holdings.save(holding);
        Transaction transaction = transactions.saveAndFlush(new Transaction(user, stock,
                TransactionType.BUY, quantity, stock.getCurrentPrice(), amount.setScale(2)));
        return response(transaction, wallet);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TradeResponse sellStock(Long userId, Long stockId, BigDecimal quantity) {
        if (quantity == null || quantity.signum() <= 0 || quantity.stripTrailingZeros().scale() > 4) {
            throw new BusinessRuleException("Quantity must be positive with at most four decimals.");
        }
        User user = requireUser(userId);
        Wallet wallet = requireWallet(userId);
        Stock stock = requireStock(stockId);
        Holding holding = holdings.findByUserIdAndStockId(userId, stockId)
                .orElseThrow(() -> new InsufficientHoldingsException("You do not own this stock."));
        holding.remove(quantity);
        BigDecimal amount = quantity.multiply(stock.getCurrentPrice()).setScale(2, RoundingMode.HALF_UP);
        if (amount.signum() == 0) {
            throw new BusinessRuleException("Sale proceeds must be at least ₹0.01.");
        }
        wallet.credit(amount);
        wallets.saveAndFlush(wallet);
        if (holding.getQuantity().signum() == 0) {
            holdings.delete(holding);
        } else {
            holdings.save(holding);
        }
        Transaction transaction = transactions.saveAndFlush(new Transaction(user, stock,
                TransactionType.SELL, quantity, stock.getCurrentPrice(), amount));
        return response(transaction, wallet);
    }

    private User requireUser(Long id) {
        return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    private Wallet requireWallet(Long id) {
        return wallets.findByUserId(id).orElseThrow(() -> new ResourceNotFoundException("Wallet not found."));
    }

    private Stock requireStock(Long id) {
        return stocks.findById(id).orElseThrow(() -> new ResourceNotFoundException("Stock not found."));
    }

    private TradeResponse response(Transaction tx, Wallet wallet) {
        return new TradeResponse(tx.getId(), tx.getStock().getId(), tx.getStock().getSymbol(),
                tx.getType().name(), tx.getQuantity(), tx.getPrice(), tx.getAmount(), wallet.getBalance());
    }
}
