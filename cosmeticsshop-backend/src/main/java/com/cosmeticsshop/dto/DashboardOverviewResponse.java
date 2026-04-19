package com.cosmeticsshop.dto;

import java.util.List;
import java.util.Map;

public class DashboardOverviewResponse {

    private final String role;
    private final double totalRevenue;
    private final long totalOrders;
    private final long totalUsers;
    private final long totalProducts;
    private final long lowStockProducts;
    private final List<TopProductResponse> topProducts;
    private final List<Map<String, Object>> salesTrend;
    private final List<Map<String, Object>> categoryShare;

    public DashboardOverviewResponse(
            String role,
            double totalRevenue,
            long totalOrders,
            long totalUsers,
            long totalProducts,
            long lowStockProducts,
            List<TopProductResponse> topProducts,
            List<Map<String, Object>> salesTrend,
            List<Map<String, Object>> categoryShare
    ) {
        this.role = role;
        this.totalRevenue = totalRevenue;
        this.totalOrders = totalOrders;
        this.totalUsers = totalUsers;
        this.totalProducts = totalProducts;
        this.lowStockProducts = lowStockProducts;
        this.topProducts = topProducts;
        this.salesTrend = salesTrend;
        this.categoryShare = categoryShare;
    }

    public String getRole() {
        return role;
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }

    public long getTotalOrders() {
        return totalOrders;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public long getTotalProducts() {
        return totalProducts;
    }

    public long getLowStockProducts() {
        return lowStockProducts;
    }

    public List<TopProductResponse> getTopProducts() {
        return topProducts;
    }

    public List<Map<String, Object>> getSalesTrend() {
        return salesTrend;
    }

    public List<Map<String, Object>> getCategoryShare() {
        return categoryShare;
    }
}
