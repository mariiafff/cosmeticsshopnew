package com.cosmeticsshop.model;

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

@Entity
@Table(
        name = "products",
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

    @Column(nullable = false, length = 120)
    private String name;

    private double price;

    @Column(length = 1000)
    private String description;

    private Long sellerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @Column(length = 60)
    private String sku;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    private Integer stockQuantity = 0;

    @Column(length = 30)
    private String status = "ACTIVE";

    private Double averageRating = 0.0;

    public Product() {}

    public Product(String name, double price, String description, Long sellerId) {
        this.name = name;
        this.price = price;
        this.description = description;
        this.sellerId = sellerId;
    }

    // getters and setters
    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }

    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getAverageRating() { return averageRating; }
    public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }
}
