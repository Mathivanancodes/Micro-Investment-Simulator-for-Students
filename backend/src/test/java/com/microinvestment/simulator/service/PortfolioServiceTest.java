package com.microinvestment.simulator.service;

import com.microinvestment.simulator.dto.PortfolioSummaryDto;
import com.microinvestment.simulator.entity.Holding;
import com.microinvestment.simulator.entity.PriceHistory;
import com.microinvestment.simulator.entity.Stock;
import com.microinvestment.simulator.entity.Transaction;
import com.microinvestment.simulator.entity.TransactionType;
import com.microinvestment.simulator.entity.User;
import com.microinvestment.simulator.entity.Wallet;
import com.microinvestment.simulator.repository.HoldingRepository;
import com.microinvestment.simulator.repository.PriceHistoryRepository;
import com.microinvestment.simulator.repository.TransactionRepository;
import com.microinvestment.simulator.repository.UserRepository;
import com.microinvestment.simulator.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {
    @Mock private UserRepository users;
    @Mock private WalletRepository wallets;
    @Mock private HoldingRepository holdings;
    @Mock private TransactionRepository transactions;
    @Mock private PriceHistoryRepository history;
    private PortfolioService service;
    private User user;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        service = new PortfolioService(users, wallets, holdings, transactions, history);
        user = new User("Student", "portfolio@example.test", "hash");
        ReflectionTestUtils.setField(user, "id", 1L);
        ReflectionTestUtils.setField(user, "createdAt", LocalDate.now().minusDays(5).atStartOfDay());
        wallet = new Wallet(user);
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(wallets.findByUserId(1L)).thenReturn(Optional.of(wallet));
    }

    @Test
    void emptyPortfolioHasZeroProfitAndSafeZeroPercentage() {
        when(holdings.findByUserId(1L)).thenReturn(List.of());
        when(transactions.findByUserIdOrderByCreatedAtAsc(1L)).thenReturn(List.of());

        PortfolioSummaryDto result = service.getPortfolioSummary(1L);

        assertThat(result.walletBalance()).isEqualByComparingTo("1000.00");
        assertThat(result.holdingsValue()).isEqualByComparingTo("0");
        assertThat(result.totalValue()).isEqualByComparingTo("1000.00");
        assertThat(result.invested()).isEqualByComparingTo("0");
        assertThat(result.pnl()).isEqualByComparingTo("0");
        assertThat(result.pnlPercentage()).isEqualByComparingTo("0");
        assertThat(result.historicalValue()).hasSize(7);
    }

    @Test
    void markToMarketAndHistoricalReplayUseThePricesAndHoldingsForEachDate() {
        Stock stock = new Stock("TEST", "Test Company", new BigDecimal("125.00"));
        ReflectionTestUtils.setField(stock, "id", 2L);
        Holding holding = new Holding(user, stock, new BigDecimal("1.0000"), new BigDecimal("100.00"));
        wallet.debit(new BigDecimal("100.00"));
        Transaction purchase = new Transaction(user, stock, TransactionType.BUY,
                new BigDecimal("1.0000"), new BigDecimal("100.00"), new BigDecimal("100.00"));
        LocalDateTime yesterday = LocalDate.now().minusDays(1).atTime(12, 0);
        ReflectionTestUtils.setField(purchase, "createdAt", yesterday);
        when(holdings.findByUserId(1L)).thenReturn(List.of(holding));
        when(transactions.findByUserIdOrderByCreatedAtAsc(1L)).thenReturn(List.of(purchase));
        when(history.findByStockIdAndRecordedAtAfterOrderByRecordedAtAsc(eq(2L), any())).thenReturn(List.of(
                new PriceHistory(stock, new BigDecimal("110.00"), yesterday),
                new PriceHistory(stock, new BigDecimal("125.00"), LocalDate.now().atTime(12, 0))));

        PortfolioSummaryDto result = service.getPortfolioSummary(1L);

        assertThat(result.holdingsValue()).isEqualByComparingTo("125.00");
        assertThat(result.totalValue()).isEqualByComparingTo("1025.00");
        assertThat(result.invested()).isEqualByComparingTo("100.00");
        assertThat(result.pnl()).isEqualByComparingTo("25.00");
        assertThat(result.pnlPercentage()).isEqualByComparingTo("25.00");
        assertThat(result.historicalValue().get(4).value()).isEqualByComparingTo("1000.00");
        assertThat(result.historicalValue().get(5).value()).isEqualByComparingTo("1010.00");
        assertThat(result.historicalValue().get(6).value()).isEqualByComparingTo("1025.00");
    }
}
