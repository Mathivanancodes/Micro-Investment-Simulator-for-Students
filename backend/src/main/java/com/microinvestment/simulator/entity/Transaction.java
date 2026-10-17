package com.microinvestment.simulator.entity;

import com.microinvestment.simulator.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "transactions", indexes = @Index(name = "idx_transaction_user_created", columnList = "user_id,created_at"))
public class Transaction extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id")
    private Stock stock;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    protected Transaction() { }

    public Transaction(User user, Stock stock, TransactionType type, BigDecimal quantity, BigDecimal price, BigDecimal amount) {
        this.user = Objects.requireNonNull(user, "User is required");
        this.type = Objects.requireNonNull(type, "Transaction type is required");
        if (type != TransactionType.DEPOSIT && stock == null) {
            throw new BusinessRuleException("A stock is required for BUY and SELL transactions");
        }
        if (type == TransactionType.DEPOSIT && stock != null) {
            throw new BusinessRuleException("A deposit cannot reference a stock");
        }
        this.stock = stock;
        this.quantity = EntityRules.decimal(quantity, 4, type == TransactionType.DEPOSIT, "Quantity");
        this.price = EntityRules.decimal(price, 2, type == TransactionType.DEPOSIT, "Price");
        this.amount = EntityRules.decimal(amount, 2, false, "Amount");
        if (type == TransactionType.DEPOSIT && (this.quantity.signum() != 0 || this.price.signum() != 0)) {
            throw new BusinessRuleException("Deposits must have zero quantity and price");
        }
    }

    public User getUser() { return user; }
    public Stock getStock() { return stock; }
    public TransactionType getType() { return type; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getAmount() { return amount; }
}
