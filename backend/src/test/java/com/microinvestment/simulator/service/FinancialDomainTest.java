package com.microinvestment.simulator.service;

import com.microinvestment.simulator.entity.Holding;
import com.microinvestment.simulator.entity.Stock;
import com.microinvestment.simulator.entity.Transaction;
import com.microinvestment.simulator.entity.TransactionType;
import com.microinvestment.simulator.entity.User;
import com.microinvestment.simulator.entity.Wallet;
import com.microinvestment.simulator.exception.BusinessRuleException;
import com.microinvestment.simulator.exception.InsufficientBalanceException;
import com.microinvestment.simulator.exception.InsufficientHoldingsException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FinancialDomainTest {
    private final User user = new User("Student", "STUDENT@example.test", "bcrypt-hash");
    private final Stock stock = new Stock("test", "Test Company", new BigDecimal("100.00"));

    @Test
    void walletRejectsNegativeZeroAndUnrepresentableMoneyWithoutChangingBalance() {
        Wallet wallet = new Wallet(user);
        for (String invalid : new String[]{"0", "-1", "0.001", "99999999999999.99"}) {
            assertThatThrownBy(() -> wallet.credit(new BigDecimal(invalid))).isInstanceOf(BusinessRuleException.class);
            assertThat(wallet.getBalance()).isEqualByComparingTo("1000.00");
        }
        assertThatThrownBy(() -> wallet.debit(new BigDecimal("1000.01"))).isInstanceOf(InsufficientBalanceException.class);
        assertThat(wallet.getBalance()).isEqualByComparingTo("1000.00");
    }

    @Test
    void walletCanBeFullySpentAndCreditedAgain() {
        Wallet wallet = new Wallet(user);
        wallet.debit(new BigDecimal("1000.00"));
        assertThat(wallet.getBalance()).isEqualByComparingTo("0.00");
        wallet.credit(new BigDecimal("0.01"));
        assertThat(wallet.getBalance()).isEqualByComparingTo("0.01");
    }

    @Test
    void holdingCannotOversellOrSilentlyTruncateFractionalShares() {
        Holding holding = new Holding(user, stock, new BigDecimal("1.0000"), new BigDecimal("100.00"));
        assertThatThrownBy(() -> holding.remove(new BigDecimal("1.0001"))).isInstanceOf(InsufficientHoldingsException.class);
        assertThatThrownBy(() -> holding.remove(new BigDecimal("0.00001"))).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> holding.add(new BigDecimal("0.00001"), new BigDecimal("100.00"))).isInstanceOf(BusinessRuleException.class);
        assertThat(holding.getQuantity()).isEqualByComparingTo("1.0000");
    }

    @Test
    void holdingUsesWeightedAverageAndRetainsCostBasisAfterPartialSale() {
        Holding holding = new Holding(user, stock, new BigDecimal("1.0000"), new BigDecimal("100.00"));
        holding.add(new BigDecimal("3.0000"), new BigDecimal("200.00"));
        assertThat(holding.getAvgBuyPrice()).isEqualByComparingTo("175.00");
        holding.remove(new BigDecimal("2.0000"));
        assertThat(holding.getQuantity()).isEqualByComparingTo("2.0000");
        assertThat(holding.getAvgBuyPrice()).isEqualByComparingTo("175.00");
    }

    @Test
    void persistedIdentityKeepsHashCollectionsStableAndDoesNotLeakCredentials() {
        User sameRecord = new User("Other Name", "other@example.test", "secret-hash");
        assertThat(user).isNotEqualTo(sameRecord);
        Set<User> users = new HashSet<>();
        users.add(user);
        ReflectionTestUtils.setField(user, "id", 1L);
        ReflectionTestUtils.setField(sameRecord, "id", 1L);
        assertThat(users).contains(user).contains(sameRecord);
        assertThat(user).isEqualTo(sameRecord);
        assertThat(user.toString()).doesNotContain("bcrypt-hash", "STUDENT@example.test");
        assertThat(user.getEmail()).isEqualTo("student@example.test");
        assertThat(stock.getSymbol()).isEqualTo("TEST");
    }

    @Test
    void depositsHaveNoStockAndTradesMustHaveOne() {
        Transaction deposit = new Transaction(user, null, TransactionType.DEPOSIT,
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("50.00"));
        assertThat(deposit.getStock()).isNull();
        assertThat(deposit.getQuantity()).isEqualByComparingTo("0");
        assertThatThrownBy(() -> new Transaction(user, null, TransactionType.BUY,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> new Transaction(user, stock, TransactionType.DEPOSIT,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE)).isInstanceOf(BusinessRuleException.class);
    }
}
