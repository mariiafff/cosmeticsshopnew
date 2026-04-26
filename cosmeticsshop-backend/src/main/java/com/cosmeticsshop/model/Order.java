package com.cosmeticsshop.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "orders",
        schema = "public",
        indexes = {
                @Index(name = "idx_order_user", columnList = "user_id"),
                @Index(name = "idx_order_store", columnList = "store_id"),
                @Index(name = "idx_order_status", columnList = "status"),
                @Index(name = "idx_order_created", columnList = "created_at")
        }
)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @Column(name = "grand_total")
    private double totalPrice;

    @Column(name = "source_order_id")
    private String sourceOrderId;

    @Column(name = "invoice_no")
    private String orderNumber;

    @Column(name = "increment_id")
    private String incrementId;

    @Column(name = "payment_method")
    private String paymentMethod;

    private String status = "PLACED";

    @Column(name = "fulfilment")
    private String fulfillmentStatus = "PENDING";

    @Column(name = "sales_channel")
    private String salesChannel = "WEB";

    @Column(name = "ship_service_level")
    private String shipServiceLevel = "STANDARD";

    @Transient
    private String shipmentStatus;

    @Column(name = "currency_code")
    private String currencyCode = "USD";

    @Column(name = "normalized_grand_total_usd")
    private Double normalizedGrandTotalUsd;

    @Column(name = "order_date")
    private LocalDateTime orderDate = LocalDateTime.now();

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Transient
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Order() {}

    public Order(User user, double totalPrice) {
        this.user = user;
        this.totalPrice = totalPrice;
    }

    // getters and setters
    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }

    public String getSourceOrderId() { return sourceOrderId; }
    public void setSourceOrderId(String sourceOrderId) { this.sourceOrderId = sourceOrderId; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public String getIncrementId() { return incrementId; }
    public void setIncrementId(String incrementId) { this.incrementId = incrementId; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getFulfillmentStatus() { return fulfillmentStatus; }
    public void setFulfillmentStatus(String fulfillmentStatus) { this.fulfillmentStatus = fulfillmentStatus; }

    public String getSalesChannel() { return salesChannel; }
    public void setSalesChannel(String salesChannel) { this.salesChannel = salesChannel; }

    public String getShipServiceLevel() { return shipServiceLevel; }
    public void setShipServiceLevel(String shipServiceLevel) { this.shipServiceLevel = shipServiceLevel; }

    public String getShipmentStatus() { return shipmentStatus; }
    public void setShipmentStatus(String shipmentStatus) { this.shipmentStatus = shipmentStatus; }

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public Double getNormalizedGrandTotalUsd() { return normalizedGrandTotalUsd; }
    public void setNormalizedGrandTotalUsd(Double normalizedGrandTotalUsd) { this.normalizedGrandTotalUsd = normalizedGrandTotalUsd; }

    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
