package com.microinvestment.simulator.service;

import com.microinvestment.simulator.dto.LeaderboardEntryDto;
import com.microinvestment.simulator.model.Holding;
import com.microinvestment.simulator.model.Stock;
import com.microinvestment.simulator.model.Wallet;
import com.microinvestment.simulator.repository.HoldingRepository;
import com.microinvestment.simulator.repository.StockRepository;
import com.microinvestment.simulator.repository.TransactionRepository;
import com.microinvestment.simulator.repository.WalletRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class LeaderboardService {

    private final WalletRepository walletRepository;
    private final HoldingRepository holdingRepository;
    private final StockRepository stockRepository;
    private final TransactionRepository transactionRepository;

    @Autowired
    public LeaderboardService(WalletRepository walletRepository,
                              HoldingRepository holdingRepository,
                              StockRepository stockRepository,
                              TransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.holdingRepository = holdingRepository;
        this.stockRepository = stockRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Calculates rankings across all users based on percentage returns of this period.
     */
    @Transactional(readOnly = true)
    public List<LeaderboardEntryDto> getLeaderboard() {
        log.info("Calculating classroom leaderboard rankings...");
        List<Wallet> wallets = walletRepository.findAll();
        List<LeaderboardEntryDto> entries = new ArrayList<>();

        for (Wallet wallet : wallets) {
            BigDecimal holdingsVal = BigDecimal.ZERO;
            List<Holding> holdings = holdingRepository.findByUserId(wallet.getUser().getId());
            
            for (Holding holding : holdings) {
                Optional<Stock> stockOpt = stockRepository.findBySymbol(holding.getStockSymbol());
                BigDecimal currentPrice = stockOpt.map(Stock::getCurrentPrice).orElse(holding.getAveragePrice());
                BigDecimal assetVal = holding.getQuantity().multiply(currentPrice);
                holdingsVal = holdingsVal.add(assetVal);
            }

            BigDecimal currentTotalValue = wallet.getBalance().add(holdingsVal);
            BigDecimal startBalance = wallet.getPeriodStartBalance();

            // Handle divide by zero edge case if starting balance was somehow initialized to 0
            if (startBalance.compareTo(BigDecimal.ZERO) <= 0) {
                startBalance = new BigDecimal("10000.00");
            }

            // ReturnRate = ((currentValue - startingValue) / startingValue) * 100
            BigDecimal returnDiff = currentTotalValue.subtract(startBalance);
            BigDecimal returnRate = returnDiff.multiply(BigDecimal.valueOf(100))
                    .divide(startBalance, 2, RoundingMode.HALF_UP);

            int tradesCount = transactionRepository.findByUserIdOrderByTimestampDesc(wallet.getUser().getId()).size();

            entries.add(LeaderboardEntryDto.builder()
                    .userId(wallet.getUser().getId())
                    .username(wallet.getUser().getUsername())
                    .totalValue(currentTotalValue.setScale(2, RoundingMode.HALF_UP))
                    .returnRate(returnRate)
                    .tradesCount(tradesCount)
                    .build());
        }

        // Sort descending by return rate
        entries.sort(Comparator.comparing(LeaderboardEntryDto::getReturnRate).reversed());

        // Assign ranks (1-indexed)
        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).setRank(i + 1);
        }

        return entries;
    }

    /**
     * Resets the leaderboard starting balance for all users to their current total portfolio value.
     */
    @Transactional
    public void resetLeaderboard() {
        log.info("Resetting leaderboard baseline starting balances...");
        List<Wallet> wallets = walletRepository.findAll();
        
        for (Wallet wallet : wallets) {
            BigDecimal holdingsVal = BigDecimal.ZERO;
            List<Holding> holdings = holdingRepository.findByUserId(wallet.getUser().getId());
            
            for (Holding holding : holdings) {
                Optional<Stock> stockOpt = stockRepository.findBySymbol(holding.getStockSymbol());
                BigDecimal currentPrice = stockOpt.map(Stock::getCurrentPrice).orElse(holding.getAveragePrice());
                BigDecimal assetVal = holding.getQuantity().multiply(currentPrice);
                holdingsVal = holdingsVal.add(assetVal);
            }

            BigDecimal currentTotalValue = wallet.getBalance().add(holdingsVal);
            wallet.setPeriodStartBalance(currentTotalValue.setScale(2, RoundingMode.HALF_UP));
            walletRepository.save(wallet);
        }
        
        log.info("Leaderboard period reset completed successfully.");
    }
}
