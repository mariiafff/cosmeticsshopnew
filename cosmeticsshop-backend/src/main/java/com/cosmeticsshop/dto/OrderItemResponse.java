package com.cosmeticsshop.dto;

public class OrderItemResponse {

    private Long productId;
    private String productName;
    private String category;
    private Integer quantity;
    private Double price;

    public OrderItemResponse(Long productId, String productName, String category, Integer quantity, Double price) {
        this.productId = productId;
        this.productName = productName;
        this.category = category;
        this.quantity = quantity;
        this.price = price;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getCategory() {
        return category;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Double getPrice() {
        return price;
    }
}
