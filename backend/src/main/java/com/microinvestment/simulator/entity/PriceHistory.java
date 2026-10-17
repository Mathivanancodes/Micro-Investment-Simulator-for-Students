package com.microinvestment.simulator.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

@Entity
@Table(name = "price_history", indexes = @Index(name = "idx_price_history_stock_time", columnList = "stock_id,recorded_at"))
public class PriceHistory extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private LocalDateTime recordedAt;

    protected PriceHistory() { }

    public PriceHistory(Stock stock, BigDecimal price) {
        this(stock, price, LocalDateTime.now(ZoneOffset.UTC));
    }

    public PriceHistory(Stock stock, BigDecimal price, LocalDateTime recordedAt) {
        this.stock = Objects.requireNonNull(stock, "Stock is required");
        this.price = EntityRules.decimal(price, 2, false, "Price");
        this.recordedAt = Objects.requireNonNull(recordedAt, "Recording time is required");
    }

    public Stock getStock() { return stock; }
    public BigDecimal getPrice() { return price; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
}
