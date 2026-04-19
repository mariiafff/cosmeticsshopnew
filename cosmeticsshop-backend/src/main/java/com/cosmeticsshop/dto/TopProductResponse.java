package com.cosmeticsshop.dto;

public class TopProductResponse {

    private final Long productId;
    private final String productName;
    private final Long totalQuantitySold;
    private final Double totalRevenue;

    public TopProductResponse(Long productId, String productName, Long totalQuantitySold, Double totalRevenue) {
        this.productId = productId;
        this.productName = productName;
        this.totalQuantitySold = totalQuantitySold;
        this.totalRevenue = totalRevenue;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public Long getTotalQuantitySold() {
        return totalQuantitySold;
    }

    public Double getTotalRevenue() {
        return totalRevenue;
    }
}
