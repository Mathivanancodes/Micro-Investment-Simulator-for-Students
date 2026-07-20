package com.microinvestment.simulator.service;

import com.microinvestment.simulator.dto.HoldingSummaryDto;
import com.microinvestment.simulator.dto.PortfolioSummaryDto;
import com.microinvestment.simulator.exception.UserNotFoundException;
import com.microinvestment.simulator.model.*;
import com.microinvestment.simulator.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class PortfolioService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final HoldingRepository holdingRepository;
    private final StockRepository stockRepository;
    private final TransactionRepository transactionRepository;
    private final PriceHistoryRepository priceHistoryRepository;

    @Autowired
    public PortfolioService(UserRepository userRepository,
                            WalletRepository walletRepository,
                            HoldingRepository holdingRepository,
                            StockRepository stockRepository,
                            TransactionRepository transactionRepository,
                            PriceHistoryRepository priceHistoryRepository) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.holdingRepository = holdingRepository;
        this.stockRepository = stockRepository;
        this.transactionRepository = transactionRepository;
        this.priceHistoryRepository = priceHistoryRepository;
    }

    /**
     * Aggregates the user's complete portfolio summary.
     */
    @Transactional(readOnly = true)
    public PortfolioSummaryDto getPortfolioSummary(Long userId) {
        log.info("Aggregating portfolio summary for user {}", userId);

        // 1. Validate User
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        // 2. Fetch Wallet
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException("Wallet not found for user: " + userId));

        // 3. Fetch Holdings
        List<Holding> holdings = holdingRepository.findByUserId(userId);
        List<HoldingSummaryDto> holdingDtos = new ArrayList<>();
        BigDecimal totalHoldingsValue = BigDecimal.ZERO;

        // 4. Map Holdings to DTOs and calculate totals
        for (Holding holding : holdings) {
            Optional<Stock> stockOpt = stockRepository.findBySymbol(holding.getStockSymbol());
            BigDecimal currentPrice = stockOpt.map(Stock::getCurrentPrice).orElse(holding.getAveragePrice());

            BigDecimal quantity = holding.getQuantity();
            BigDecimal averagePrice = holding.getAveragePrice();
            
            BigDecimal marketValue = quantity.multiply(currentPrice).setScale(2, RoundingMode.HALF_UP);
            BigDecimal costBasis = quantity.multiply(averagePrice).setScale(2, RoundingMode.HALF_UP);
            BigDecimal pnl = marketValue.subtract(costBasis);
            
            BigDecimal pnlPercentage = BigDecimal.ZERO;
            if (costBasis.compareTo(BigDecimal.ZERO) > 0) {
                pnlPercentage = pnl.multiply(BigDecimal.valueOf(100))
                        .divide(costBasis, 2, RoundingMode.HALF_UP);
            }

            HoldingSummaryDto dto = HoldingSummaryDto.builder()
                    .symbol(holding.getStockSymbol())
                    .quantity(quantity)
                    .averagePrice(averagePrice)
                    .currentPrice(currentPrice)
                    .marketValue(marketValue)
                    .pnl(pnl)
                    .pnlPercentage(pnlPercentage)
                    .build();
            
            holdingDtos.add(dto);
            totalHoldingsValue = totalHoldingsValue.add(marketValue);
        }

        BigDecimal totalPortfolioValue = wallet.getBalance().add(totalHoldingsValue);

        // 5. Generate 7-day historical backtest values for charting
        List<PortfolioSummaryDto.HistoryPoint> historyPoints = generatePortfolioHistory(holdings, wallet.getBalance());

        return PortfolioSummaryDto.builder()
                .userId(userId)
                .walletBalance(wallet.getBalance())
                .holdingsValue(totalHoldingsValue)
                .totalValue(totalPortfolioValue)
                .holdings(holdingDtos)
                .historicalValue(historyPoints)
                .build();
    }

    /**
     * Retrieves all transactions logged for a user, ordered newest first.
     */
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionHistory(Long userId) {
        log.info("Fetching transaction history for user {}", userId);
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("User not found with id: " + userId);
        }
        return transactionRepository.findByUserIdOrderByTimestampDesc(userId);
    }

    /**
     * Generates a 7-day historical backtest of the user's current holdings
     * combined with historical stock prices to build line charts.
     */
    private List<PortfolioSummaryDto.HistoryPoint> generatePortfolioHistory(List<Holding> holdings, BigDecimal currentWalletBalance) {
        List<PortfolioSummaryDto.HistoryPoint> points = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");
        LocalDate today = LocalDate.now();

        // Generate points for the last 7 days
        for (int i = 6; i >= 0; i--) {
            LocalDate targetDate = today.minusDays(i);
            BigDecimal dayHoldingsValue = BigDecimal.ZERO;

            for (Holding holding : holdings) {
                BigDecimal historicalPrice = getHistoricalStockPrice(holding.getStockSymbol(), targetDate);
                BigDecimal assetValue = holding.getQuantity().multiply(historicalPrice);
                dayHoldingsValue = dayHoldingsValue.add(assetValue);
            }

            BigDecimal dayTotalValue = currentWalletBalance.add(dayHoldingsValue).setScale(2, RoundingMode.HALF_UP);
            
            points.add(PortfolioSummaryDto.HistoryPoint.builder()
                    .date(targetDate.format(formatter))
                    .value(dayTotalValue)
                    .build());
        }
        return points;
    }

    /**
     * Helper to find stock price closest to targetDate in PriceHistory.
     * Falls back to current Stock price if no historical data is found.
     */
    private BigDecimal getHistoricalStockPrice(String symbol, LocalDate date) {
        LocalDateTime dayEnd = date.atTime(23, 59, 59);
        List<PriceHistory> history = priceHistoryRepository.findByStockSymbolOrderByTimestampDesc(symbol);
        
        // Find the closest historical price before or on the target date
        for (PriceHistory record : history) {
            if (record.getTimestamp().isBefore(dayEnd)) {
                return record.getPrice();
            }
        }

        // Fallback: check Stock current price
        return stockRepository.findBySymbol(symbol)
                .map(Stock::getCurrentPrice)
                .orElse(BigDecimal.ZERO);
    }
}
