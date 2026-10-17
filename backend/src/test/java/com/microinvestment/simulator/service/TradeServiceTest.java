package com.microinvestment.simulator.service;

import com.microinvestment.simulator.dto.TradeResponse;
import com.microinvestment.simulator.exception.BusinessRuleException;
import com.microinvestment.simulator.exception.InsufficientBalanceException;
import com.microinvestment.simulator.exception.InsufficientHoldingsException;
import com.microinvestment.simulator.exception.ResourceNotFoundException;
import com.microinvestment.simulator.entity.Holding;
import com.microinvestment.simulator.entity.Stock;
import com.microinvestment.simulator.entity.Transaction;
import com.microinvestment.simulator.entity.TransactionType;
import com.microinvestment.simulator.entity.User;
import com.microinvestment.simulator.entity.Wallet;
import com.microinvestment.simulator.repository.HoldingRepository;
import com.microinvestment.simulator.repository.StockRepository;
import com.microinvestment.simulator.repository.TransactionRepository;
import com.microinvestment.simulator.repository.UserRepository;
import com.microinvestment.simulator.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeServiceTest {
    @Mock private UserRepository users;
    @Mock private WalletRepository wallets;
    @Mock private StockRepository stocks;
    @Mock private HoldingRepository holdings;
    @Mock private TransactionRepository transactions;

    private TradeService service;
    private User user;
    private Wallet wallet;
    private Stock stock;

    @BeforeEach
    void setUp() {
        service = new TradeService(users, wallets, stocks, holdings, transactions);
        user = new User("Test Student", "student@example.test", "hashed-password");
        ReflectionTestUtils.setField(user, "id", 1L);
        wallet = new Wallet(user);
        ReflectionTestUtils.setField(wallet, "id", 2L);
        stock = new Stock("TEST", "Test Company", decimal("100.00"));
        ReflectionTestUtils.setField(stock, "id", 3L);
    }

    @Test
    void buyDebitsWalletCreatesHoldingAndRecordsLedger() {
        stubAccount();
        when(holdings.findByUserIdAndStockId(1L, 3L)).thenReturn(Optional.empty());
        stubSavedTransaction();

        TradeResponse response = service.buyStock(1L, 3L, decimal("125.00"));

        assertThat(response.type()).isEqualTo("BUY");
        assertThat(response.quantity()).isEqualByComparingTo("1.2500");
        assertThat(response.amount()).isEqualByComparingTo("125.00");
        assertThat(response.remainingBalance()).isEqualByComparingTo("875.00");
        assertThat(response.transactionId()).isEqualTo(10L);
        ArgumentCaptor<Holding> holding = ArgumentCaptor.forClass(Holding.class);
        verify(holdings).save(holding.capture());
        assertThat(holding.getValue().getQuantity()).isEqualByComparingTo("1.2500");
        assertThat(holding.getValue().getAvgBuyPrice()).isEqualByComparingTo("100.00");
        verify(wallets).saveAndFlush(wallet);
        ArgumentCaptor<Transaction> transaction = ArgumentCaptor.forClass(Transaction.class);
        verify(transactions).saveAndFlush(transaction.capture());
        assertThat(transaction.getValue().getType()).isEqualTo(TransactionType.BUY);
        assertThat(transaction.getValue().getAmount()).isEqualByComparingTo("125.00");
    }

    @Test
    void buyingAgainUpdatesWeightedAverageRatherThanReplacingIt() {
        stubAccount();
        Holding holding = new Holding(user, stock, decimal("2.0000"), decimal("50.00"));
        when(holdings.findByUserIdAndStockId(1L, 3L)).thenReturn(Optional.of(holding));
        stubSavedTransaction();

        service.buyStock(1L, 3L, decimal("100.00"));

        assertThat(holding.getQuantity()).isEqualByComparingTo("3.0000");
        assertThat(holding.getAvgBuyPrice()).isEqualByComparingTo("66.67");
    }

    @Test
    void insufficientBalanceDoesNotWriteAnOrder() {
        stubAccount();
        wallet.debit(decimal("995.00"));

        assertThatThrownBy(() -> service.buyStock(1L, 3L, decimal("100.00")))
                .isInstanceOf(InsufficientBalanceException.class);

        assertThat(wallet.getBalance()).isEqualByComparingTo("5.00");
        verify(transactions, never()).saveAndFlush(any());
        verify(holdings, never()).save(any());
    }

    @Test
    void sellCreditsWalletAndReducesHolding() {
        stubAccount();
        Holding holding = new Holding(user, stock, decimal("2.0000"), decimal("80.00"));
        when(holdings.findByUserIdAndStockId(1L, 3L)).thenReturn(Optional.of(holding));
        stubSavedTransaction();

        TradeResponse response = service.sellStock(1L, 3L, decimal("0.5000"));

        assertThat(response.type()).isEqualTo("SELL");
        assertThat(response.amount()).isEqualByComparingTo("50.00");
        assertThat(wallet.getBalance()).isEqualByComparingTo("1050.00");
        assertThat(holding.getQuantity()).isEqualByComparingTo("1.5000");
        assertThat(holding.getAvgBuyPrice()).isEqualByComparingTo("80.00");
        verify(holdings).save(holding);
    }

    @Test
    void sellingAllSharesDeletesEmptyHolding() {
        stubAccount();
        Holding holding = new Holding(user, stock, decimal("1.0000"), decimal("80.00"));
        when(holdings.findByUserIdAndStockId(1L, 3L)).thenReturn(Optional.of(holding));
        stubSavedTransaction();

        service.sellStock(1L, 3L, decimal("1.0000"));

        verify(holdings).delete(holding);
        assertThat(wallet.getBalance()).isEqualByComparingTo("1100.00");
    }

    @Test
    void oversellingDoesNotCreditWalletOrWriteLedger() {
        stubAccount();
        when(holdings.findByUserIdAndStockId(1L, 3L)).thenReturn(Optional.of(
                new Holding(user, stock, decimal("0.5000"), decimal("80.00"))));

        assertThatThrownBy(() -> service.sellStock(1L, 3L, decimal("1.0000")))
                .isInstanceOf(InsufficientHoldingsException.class);

        assertThat(wallet.getBalance()).isEqualByComparingTo("1000.00");
        verify(wallets, never()).saveAndFlush(any());
        verify(transactions, never()).saveAndFlush(any());
    }

    @Test
    void sellingWithoutHoldingFailsClearly() {
        stubAccount();
        when(holdings.findByUserIdAndStockId(1L, 3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.sellStock(1L, 3L, decimal("1.0000")))
                .isInstanceOf(InsufficientHoldingsException.class);
        verify(transactions, never()).saveAndFlush(any());
    }

    @Test
    void concurrentWalletConflictStopsHoldingAndLedgerWrites() {
        stubAccount();
        when(wallets.saveAndFlush(wallet)).thenThrow(
                new ObjectOptimisticLockingFailureException(Wallet.class, 2L));

        assertThatThrownBy(() -> service.buyStock(1L, 3L, decimal("100.00")))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        verify(holdings, never()).save(any());
        verify(transactions, never()).saveAndFlush(any());
    }

    @Test
    void missingUserIsReportedBeforeAnyWrite() {
        when(users.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buyStock(1L, 3L, decimal("100.00")))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(transactions, never()).saveAndFlush(any());
    }

    @Test
    void missingStockIsReportedBeforeAnyWrite() {
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(wallets.findByUserId(1L)).thenReturn(Optional.of(wallet));
        when(stocks.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buyStock(1L, 3L, decimal("100.00")))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(wallets, never()).saveAndFlush(any());
        verify(transactions, never()).saveAndFlush(any());
    }

    @Test
    void quantityOverloadsRoundHalfUpToFourDecimals() {
        assertThat(service.calculateQuantity(decimal("100.00"), decimal("30.00")))
                .isEqualByComparingTo("3.3333");
        assertThat(service.calculateQuantity(decimal("100.00"), decimal("6.00")))
                .isEqualByComparingTo("16.6667");
        assertThat(service.calculateQuantity(decimal("125.00"), stock))
                .isEqualByComparingTo("1.2500");
    }

    @Test
    void buyDebitsRequestedAmountWhenFractionalQuantityRequiresRounding() {
        stubAccount();
        ReflectionTestUtils.setField(stock, "currentPrice", decimal("3000.00"));
        when(holdings.findByUserIdAndStockId(1L, 3L)).thenReturn(Optional.empty());
        stubSavedTransaction();

        TradeResponse response = service.buyStock(1L, 3L, decimal("10.00"));

        assertThat(response.quantity()).isEqualByComparingTo("0.0033");
        assertThat(response.amount()).isEqualByComparingTo("10.00");
        assertThat(response.remainingBalance()).isEqualByComparingTo("990.00");
    }

    @Test
    void invalidFinancialInputsCannotReachPersistence() {
        assertThatThrownBy(() -> service.buyStock(1L, 3L, decimal("9.99")))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.buyStock(1L, 3L, decimal("500.01")))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.sellStock(1L, 3L, decimal("0.00001")))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.sellStock(1L, 3L, BigDecimal.ZERO))
                .isInstanceOf(BusinessRuleException.class);
        verify(transactions, never()).saveAndFlush(any());
    }

    private void stubAccount() {
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(wallets.findByUserId(1L)).thenReturn(Optional.of(wallet));
        when(stocks.findById(3L)).thenReturn(Optional.of(stock));
    }

    private void stubSavedTransaction() {
        when(transactions.saveAndFlush(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            ReflectionTestUtils.setField(transaction, "id", 10L);
            return transaction;
        });
    }

    private static BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }
}
