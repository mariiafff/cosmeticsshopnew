package com.cosmeticsshop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "products",
        schema = "public",
        indexes = {
                @Index(name = "idx_product_store", columnList = "store_id"),
                @Index(name = "idx_product_category", columnList = "category_id"),
                @Index(name = "idx_product_sku", columnList = "sku", unique = true)
        }
)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(length = 60)
    private String sku;

    @Column(name = "stock_code", length = 60)
    private String stockCode;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "unit_price", precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "currency_code", length = 12)
    private String currencyCode;

    @Column(name = "normalized_unit_price_usd", precision = 12, scale = 2)
    private BigDecimal normalizedUnitPriceUsd;

    @Column(name = "stock_quantity")
    private Integer stockQuantity = 0;

    @Column(length = 120)
    private String style;

    @Column(name = "product_importance", length = 60)
    private String productImportance;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Product() {}

    public Product(String name, double price, String description, Long sellerId) {
        this.name = name;
        this.unitPrice = BigDecimal.valueOf(price);
        this.description = description;
    }

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getPrice() { return unitPrice == null ? 0.0 : unitPrice.doubleValue(); }
    public void setPrice(double price) { this.unitPrice = BigDecimal.valueOf(price); }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getSellerId() { return store != null ? store.getOwnerUserId() : null; }
    public void setSellerId(Long sellerId) { }

    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getStockCode() { return stockCode; }
    public void setStockCode(String stockCode) { this.stockCode = stockCode; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }

    public String getStatus() { return null; }
    public void setStatus(String status) { }

    public Double getAverageRating() { return null; }
    public void setAverageRating(Double averageRating) { }

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public BigDecimal getNormalizedUnitPriceUsd() { return normalizedUnitPriceUsd; }
    public void setNormalizedUnitPriceUsd(BigDecimal normalizedUnitPriceUsd) { this.normalizedUnitPriceUsd = normalizedUnitPriceUsd; }

    public String getStyle() { return style; }
    public void setStyle(String style) { this.style = style; }

    public String getProductImportance() { return productImportance; }
    public void setProductImportance(String productImportance) { this.productImportance = productImportance; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
