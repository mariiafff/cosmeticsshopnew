package com.cosmeticsshop.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "shipments",
        indexes = {
                @Index(name = "idx_shipment_order", columnList = "orderId", unique = true),
                @Index(name = "idx_shipment_tracking", columnList = "trackingNumber"),
                @Index(name = "idx_shipment_status", columnList = "status")
        }
)
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;

    @Column(length = 20)
    private String warehouseBlock;

    @Column(length = 40)
    private String modeOfShipment;

    @Column(length = 80)
    private String trackingNumber;

    @Column(length = 40)
    private String status = "PROCESSING";

    private Integer customerCareCalls = 0;
    private Integer customerRating = 0;
    private Double shippingCost = 0.0;
    private Integer priorPurchases = 0;

    @Column(length = 40)
    private String productImportance;

    private Double discountOffered = 0.0;
    private LocalDateTime estimatedDeliveryAt;
    private LocalDateTime deliveredAt;

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getWarehouseBlock() {
        return warehouseBlock;
    }

    public void setWarehouseBlock(String warehouseBlock) {
        this.warehouseBlock = warehouseBlock;
    }

    public String getModeOfShipment() {
        return modeOfShipment;
    }

    public void setModeOfShipment(String modeOfShipment) {
        this.modeOfShipment = modeOfShipment;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getCustomerCareCalls() {
        return customerCareCalls;
    }

    public void setCustomerCareCalls(Integer customerCareCalls) {
        this.customerCareCalls = customerCareCalls;
    }

    public Integer getCustomerRating() {
        return customerRating;
    }

    public void setCustomerRating(Integer customerRating) {
        this.customerRating = customerRating;
    }

    public Double getShippingCost() {
        return shippingCost;
    }

    public void setShippingCost(Double shippingCost) {
        this.shippingCost = shippingCost;
    }

    public Integer getPriorPurchases() {
        return priorPurchases;
    }

    public void setPriorPurchases(Integer priorPurchases) {
        this.priorPurchases = priorPurchases;
    }

    public String getProductImportance() {
        return productImportance;
    }

    public void setProductImportance(String productImportance) {
        this.productImportance = productImportance;
    }

    public Double getDiscountOffered() {
        return discountOffered;
    }

    public void setDiscountOffered(Double discountOffered) {
        this.discountOffered = discountOffered;
    }

    public LocalDateTime getEstimatedDeliveryAt() {
        return estimatedDeliveryAt;
    }

    public void setEstimatedDeliveryAt(LocalDateTime estimatedDeliveryAt) {
        this.estimatedDeliveryAt = estimatedDeliveryAt;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }
}
