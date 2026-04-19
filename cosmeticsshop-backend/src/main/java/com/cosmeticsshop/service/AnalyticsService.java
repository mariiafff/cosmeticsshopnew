package com.cosmeticsshop.service;

import com.cosmeticsshop.dto.TopProductResponse;
import com.cosmeticsshop.repository.OrderItemRepository;
import com.cosmeticsshop.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnalyticsService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public AnalyticsService(OrderRepository orderRepository, OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
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
}
