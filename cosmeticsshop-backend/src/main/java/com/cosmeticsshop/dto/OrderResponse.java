package com.cosmeticsshop.dto;

import com.cosmeticsshop.model.Order;

import java.time.LocalDateTime;
import java.util.List;

public class OrderResponse {

    private Long id;
    private String orderNumber;
    private Double totalPrice;
    private Double totalAmount;
    private String status;
    private String shipmentStatus;
    private String paymentMethod;
    private LocalDateTime createdAt;
    private List<OrderItemResponse> items;

    public OrderResponse(Order order, List<OrderItemResponse> items) {
        this.id = order.getId();
        this.orderNumber = order.getOrderNumber();
        this.totalPrice = order.getTotalPrice();
        this.totalAmount = order.getTotalPrice();
        this.status = order.getStatus();
        this.shipmentStatus = order.getShipmentStatus();
        this.paymentMethod = order.getPaymentMethod();
        this.createdAt = order.getCreatedAt() != null ? order.getCreatedAt() : order.getOrderDate();
        this.items = items;
    }

    public Long getId() {
        return id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public Double getTotalPrice() {
        return totalPrice;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public String getShipmentStatus() {
        return shipmentStatus;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<OrderItemResponse> getItems() {
        return items;
    }
}
