package com.cosmeticsshop.service;

import com.cosmeticsshop.dto.DashboardOverviewResponse;
import com.cosmeticsshop.dto.TopProductResponse;
import com.cosmeticsshop.repository.OrderItemRepository;
import com.cosmeticsshop.repository.OrderRepository;
import com.cosmeticsshop.repository.ProductRepository;
import com.cosmeticsshop.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AnalyticsService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public AnalyticsService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            ProductRepository productRepository,
            UserRepository userRepository
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    public Double getTotalRevenue() {
        return orderRepository.findTotalRevenue();
    }

    public List<TopProductResponse> getTopSellingProducts() {
        return orderItemRepository.findTopSellingProducts();
    }

    public Long getOrdersCount() {
        return orderRepository.findTotalOrdersCount();
    }

    public List<Map<String, Object>> getSalesTrend() {
        return orderRepository.findMonthlyRevenue();
    }

    public List<Map<String, Object>> getCategoryShare() {
        return orderItemRepository.findCategoryRevenue();
    }

    public DashboardOverviewResponse getOverview(String role) {
        return new DashboardOverviewResponse(
                role,
                getTotalRevenue() == null ? 0.0 : getTotalRevenue(),
                getOrdersCount() == null ? 0L : getOrdersCount(),
                userRepository.count(),
                productRepository.count(),
                productServiceLowStock(),
                getTopSellingProducts(),
                getSalesTrend(),
                getCategoryShare()
        );
    }

    private long productServiceLowStock() {
        return productRepository.countByStockQuantityLessThanEqual(5);
    }
}
