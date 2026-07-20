package com.microinvestment.simulator.service;

import com.microinvestment.simulator.dto.TradeResponse;
import com.microinvestment.simulator.exception.InsufficientBalanceException;
import com.microinvestment.simulator.exception.InsufficientHoldingsException;
import com.microinvestment.simulator.exception.StockNotFoundException;
import com.microinvestment.simulator.exception.UserNotFoundException;
import com.microinvestment.simulator.model.*;
import com.microinvestment.simulator.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Service
@Slf4j
public class TradeService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final StockRepository stockRepository;
    private final HoldingRepository holdingRepository;
    private final TransactionRepository transactionRepository;

    @Autowired
    public TradeService(UserRepository userRepository,
                        WalletRepository walletRepository,
                        StockRepository stockRepository,
                        HoldingRepository holdingRepository,
                        TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.stockRepository = stockRepository;
        this.holdingRepository = holdingRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Executes a Buy Stock operation transactionally.
     * Uses pessimistic write lock on the Wallet record.
     */
    @Transactional
    public TradeResponse buyStock(Long userId, String symbol, BigDecimal quantity) {
        log.info("Initiating BUY order: User {}, Stock {}, Qty {}", userId, symbol, quantity);
        
        String upperSymbol = symbol.toUpperCase();

        // 1. Validate user existence
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        // 2. Lock the user's wallet to prevent concurrent double-spending race conditions
        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new UserNotFoundException("Wallet not found for user id: " + userId));

        // 3. Validate stock existence
        Stock stock = stockRepository.findBySymbol(upperSymbol)
                .orElseThrow(() -> new StockNotFoundException("Stock symbol not found: " + upperSymbol));

        // 4. Calculate total trade amount
        BigDecimal pricePerShare = stock.getCurrentPrice();
        BigDecimal totalCost = pricePerShare.multiply(quantity).setScale(2, RoundingMode.HALF_UP);

        // 5. Verify sufficient wallet balance
        if (wallet.getBalance().compareTo(totalCost) < 0) {
            log.warn("BUY failed: User {} has insufficient funds. Balance: ${}, Cost: ${}", userId, wallet.getBalance(), totalCost);
            throw new InsufficientBalanceException("Insufficient funds. Available: $" + wallet.getBalance() + ", Required: $" + totalCost);
        }

        // 6. Deduct balance from Wallet
        wallet.setBalance(wallet.getBalance().subtract(totalCost));
        walletRepository.save(wallet);

        // 7. Upsert Holding (Update avg buy price if already holding, otherwise insert new holding)
        Optional<Holding> existingHoldingOpt = holdingRepository.findByUserIdAndStockSymbol(userId, upperSymbol);
        BigDecimal finalHoldingQty;
        
        if (existingHoldingOpt.isPresent()) {
            Holding holding = existingHoldingOpt.get();
            BigDecimal currentQty = holding.getQuantity();
            BigDecimal currentAvgPrice = holding.getAveragePrice();
            
            // Calculate new average price: (CurrentValue + NewValue) / TotalQty
            BigDecimal currentValue = currentQty.multiply(currentAvgPrice);
            BigDecimal addedValue = quantity.multiply(pricePerShare);
            BigDecimal totalValue = currentValue.add(addedValue);
            
            finalHoldingQty = currentQty.add(quantity);
            BigDecimal newAvgPrice = totalValue.divide(finalHoldingQty, 2, RoundingMode.HALF_UP);
            
            holding.setQuantity(finalHoldingQty);
            holding.setAveragePrice(newAvgPrice);
            holdingRepository.save(holding);
            log.info("Updated holding for user {}: Stock {}, new Qty {}, new AvgPrice ${}", userId, upperSymbol, finalHoldingQty, newAvgPrice);
        } else {
            Holding holding = Holding.builder()
                    .user(user)
                    .stockSymbol(upperSymbol)
                    .quantity(quantity)
                    .averagePrice(pricePerShare)
                    .build();
            holdingRepository.save(holding);
            finalHoldingQty = quantity;
            log.info("Created new holding for user {}: Stock {}, Qty {}, Price ${}", userId, upperSymbol, quantity, pricePerShare);
        }

        // 8. Log audit transaction record
        Transaction transaction = Transaction.builder()
                .user(user)
                .stockSymbol(upperSymbol)
                .type(TransactionType.BUY)
                .quantity(quantity)
                .price(pricePerShare)
                .totalAmount(totalCost)
                .build();
        Transaction savedTx = transactionRepository.save(transaction);

        log.info("BUY order successfully executed. Transaction ID: {}", savedTx.getId());

        return TradeResponse.builder()
                .transactionId(savedTx.getId())
                .symbol(upperSymbol)
                .type("BUY")
                .quantity(quantity)
                .price(pricePerShare)
                .totalAmount(totalCost)
                .remainingBalance(wallet.getBalance())
                .build();
    }

    /**
     * Executes a Sell Stock operation transactionally.
     * Uses pessimistic write lock on the Wallet record.
     */
    @Transactional
    public TradeResponse sellStock(Long userId, String symbol, BigDecimal quantity) {
        log.info("Initiating SELL order: User {}, Stock {}, Qty {}", userId, symbol, quantity);

        String upperSymbol = symbol.toUpperCase();

        // 1. Validate user existence
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        // 2. Lock user's wallet to prevent balance update anomalies
        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new UserNotFoundException("Wallet not found for user id: " + userId));

        // 3. Validate stock existence
        Stock stock = stockRepository.findBySymbol(upperSymbol)
                .orElseThrow(() -> new StockNotFoundException("Stock symbol not found: " + upperSymbol));

        // 4. Validate user holds the stock
        Holding holding = holdingRepository.findByUserIdAndStockSymbol(userId, upperSymbol)
                .orElseThrow(() -> new InsufficientHoldingsException("No holdings found for stock symbol: " + upperSymbol));

        // 5. Verify user has enough shares to execute sale
        if (holding.getQuantity().compareTo(quantity) < 0) {
            log.warn("SELL failed: User {} has insufficient holdings. Held: {}, Sell request: {}", userId, holding.getQuantity(), quantity);
            throw new InsufficientHoldingsException("Insufficient holdings. Held shares: " + holding.getQuantity() + ", Requested sell: " + quantity);
        }

        // 6. Calculate total revenue
        BigDecimal pricePerShare = stock.getCurrentPrice();
        BigDecimal totalRevenue = pricePerShare.multiply(quantity).setScale(2, RoundingMode.HALF_UP);

        // 7. Credit Wallet balance
        wallet.setBalance(wallet.getBalance().add(totalRevenue));
        walletRepository.save(wallet);

        // 8. Decrement Holding (Delete record if remaining shares are 0)
        BigDecimal remainingQty = holding.getQuantity().subtract(quantity);
        if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) {
            holdingRepository.delete(holding);
            log.info("Holding deleted for user {} (fully liquidated stock: {})", userId, upperSymbol);
        } else {
            holding.setQuantity(remainingQty);
            holdingRepository.save(holding);
            log.info("Holding updated for user {}: Stock {}, remaining Qty {}", userId, upperSymbol, remainingQty);
        }

        // 9. Log audit transaction record
        Transaction transaction = Transaction.builder()
                .user(user)
                .stockSymbol(upperSymbol)
                .type(TransactionType.SELL)
                .quantity(quantity)
                .price(pricePerShare)
                .totalAmount(totalRevenue)
                .build();
        Transaction savedTx = transactionRepository.save(transaction);

        log.info("SELL order successfully executed. Transaction ID: {}", savedTx.getId());

        return TradeResponse.builder()
                .transactionId(savedTx.getId())
                .symbol(upperSymbol)
                .type("SELL")
                .quantity(quantity)
                .price(pricePerShare)
                .totalAmount(totalRevenue)
                .remainingBalance(wallet.getBalance())
                .build();
    }
}
