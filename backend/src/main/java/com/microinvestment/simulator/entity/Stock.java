package com.microinvestment.simulator.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Locale;

@Entity
@Table(name = "stocks")
public class Stock extends BaseEntity {
    @Column(nullable = false, unique = true, length = 10)
    private String symbol;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "current_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal currentPrice;

    protected Stock() { }

    public Stock(String symbol, String name, BigDecimal currentPrice) {
        this.symbol = EntityRules.text(symbol, 10, "Symbol").toUpperCase(Locale.ROOT);
        this.name = EntityRules.text(name, 100, "Stock name");
        updatePrice(currentPrice);
    }

    public void updatePrice(BigDecimal price) {
        this.currentPrice = EntityRules.decimal(price, 2, false, "Price");
    }

    public String getSymbol() { return symbol; }
    public String getName() { return name; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
}
