package com.microinvestment.simulator.entity;

import com.microinvestment.simulator.exception.InsufficientHoldingsException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Entity
@Table(name = "holdings", uniqueConstraints = @UniqueConstraint(name = "uk_holding_user_stock", columnNames = {"user_id", "stock_id"}))
public class Holding extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal quantity;

    @Column(name = "avg_buy_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal avgBuyPrice;

    protected Holding() { }

    public Holding(User user, Stock stock, BigDecimal quantity, BigDecimal avgBuyPrice) {
        this.user = Objects.requireNonNull(user, "User is required");
        this.stock = Objects.requireNonNull(stock, "Stock is required");
        this.quantity = EntityRules.decimal(quantity, 4, true, "Quantity");
        this.avgBuyPrice = EntityRules.decimal(avgBuyPrice, 2, this.quantity.signum() == 0, "Average buy price");
    }

    public void add(BigDecimal addedQuantity, BigDecimal price) {
        BigDecimal validQuantity = EntityRules.decimal(addedQuantity, 4, false, "Quantity");
        BigDecimal validPrice = EntityRules.decimal(price, 2, false, "Price");
        BigDecimal newQuantity = EntityRules.decimal(quantity.add(validQuantity), 4, false, "Quantity");
        BigDecimal newAverage = quantity.multiply(avgBuyPrice).add(validQuantity.multiply(validPrice))
                .divide(newQuantity, 2, RoundingMode.HALF_UP);
        avgBuyPrice = EntityRules.decimal(newAverage, 2, false, "Average buy price");
        quantity = newQuantity;
    }

    public void remove(BigDecimal removedQuantity) {
        BigDecimal validQuantity = EntityRules.decimal(removedQuantity, 4, false, "Quantity");
        if (quantity.compareTo(validQuantity) < 0) {
            throw new InsufficientHoldingsException("Holding quantity is insufficient for this sale");
        }
        quantity = quantity.subtract(validQuantity);
    }

    public User getUser() { return user; }
    public Stock getStock() { return stock; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getAvgBuyPrice() { return avgBuyPrice; }
}
