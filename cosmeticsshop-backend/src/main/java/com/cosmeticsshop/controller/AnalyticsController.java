package com.cosmeticsshop.controller;

import com.cosmeticsshop.dto.DashboardOverviewResponse;
import com.cosmeticsshop.dto.TopProductResponse;
import com.cosmeticsshop.service.AnalyticsService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/revenue")
    public Map<String, Double> getRevenue() {
        return Map.of("totalRevenue", analyticsService.getTotalRevenue());
    }

    @GetMapping("/top-products")
    public List<TopProductResponse> getTopProducts() {
        return analyticsService.getTopSellingProducts();
    }

    @GetMapping("/orders-count")
    public Map<String, Long> getOrdersCount() {
        return Map.of("totalOrders", analyticsService.getOrdersCount());
    }

    @GetMapping("/overview")
    public DashboardOverviewResponse getOverview(Authentication authentication) {
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(grantedAuthority -> grantedAuthority.getAuthority().replace("ROLE_", ""))
                .orElse("INDIVIDUAL");
        return analyticsService.getOverview(role);
    }

    @GetMapping("/sales-trend")
    public List<Map<String, Object>> getSalesTrend() {
        return analyticsService.getSalesTrend();
    }

    @GetMapping("/category-share")
    public List<Map<String, Object>> getCategoryShare() {
        return analyticsService.getCategoryShare();
    }
}
