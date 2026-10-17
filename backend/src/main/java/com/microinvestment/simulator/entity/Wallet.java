package com.microinvestment.simulator.entity;

import com.microinvestment.simulator.exception.InsufficientBalanceException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "wallets")
public class Wallet extends BaseEntity {
    public static final BigDecimal INITIAL_BALANCE = new BigDecimal("1000.00");

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;

    // INTERVIEW: Hibernate includes the old version in UPDATE so stale concurrent trades fail and roll back atomically.
    @Version
    @Column(nullable = false)
    private Long version;

    protected Wallet() { }

    public Wallet(User user) {
        this.user = Objects.requireNonNull(user, "User is required");
        this.balance = INITIAL_BALANCE;
    }

    // INTERVIEW: Encapsulation exposes validated operations rather than a public balance setter.
    public void credit(BigDecimal amount) {
        BigDecimal validAmount = EntityRules.decimal(amount, 2, false, "Amount");
        balance = EntityRules.decimal(balance.add(validAmount), 2, true, "Balance");
    }

    public void debit(BigDecimal amount) {
        BigDecimal validAmount = EntityRules.decimal(amount, 2, false, "Amount");
        if (balance.compareTo(validAmount) < 0) {
            throw new InsufficientBalanceException("Wallet balance is insufficient for this purchase");
        }
        balance = balance.subtract(validAmount);
    }

    public User getUser() { return user; }
    public BigDecimal getBalance() { return balance; }
    public Long getVersion() { return version; }
}
