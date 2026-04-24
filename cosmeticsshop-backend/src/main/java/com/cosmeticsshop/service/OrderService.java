package com.cosmeticsshop.service;

import com.cosmeticsshop.dto.CreateOrderRequest;
import com.cosmeticsshop.dto.OrderItemResponse;
import com.cosmeticsshop.dto.OrderResponse;
import com.cosmeticsshop.exception.ResourceNotFoundException;
import com.cosmeticsshop.model.Order;
import com.cosmeticsshop.model.OrderItem;
import com.cosmeticsshop.model.Product;
import com.cosmeticsshop.model.Shipment;
import com.cosmeticsshop.model.User;
import com.cosmeticsshop.repository.OrderItemRepository;
import com.cosmeticsshop.repository.OrderRepository;
import com.cosmeticsshop.repository.ProductRepository;
import com.cosmeticsshop.repository.ShipmentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final ShipmentRepository shipmentRepository;

    public OrderService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            ProductRepository productRepository,
            ShipmentRepository shipmentRepository
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.shipmentRepository = shipmentRepository;
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    public Order saveOrder(Order order) {
        return orderRepository.save(order);
    }

    public Order createOrder(User user, CreateOrderRequest request) {
        if (request.getProductId() == null || request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Product and quantity are required.");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.getProductId()));

        if (product.getStockQuantity() < request.getQuantity()) {
            throw new IllegalArgumentException("Not enough stock for product " + product.getName());
        }

        Order order = new Order();
        order.setUser(user);
        order.setStore(product.getStore());
        order.setOrderNumber("ORD-" + System.currentTimeMillis());
        order.setPaymentMethod(request.getPaymentMethod() == null ? "CARD" : request.getPaymentMethod());
        order.setTotalPrice(product.getPrice() * request.getQuantity());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);

        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(savedOrder);
        orderItem.setProduct(product);
        orderItem.setQuantity(request.getQuantity());
        orderItem.setPrice(product.getPrice());
        orderItemRepository.save(orderItem);

        product.setStockQuantity(product.getStockQuantity() - request.getQuantity());
        productRepository.save(product);

        Shipment shipment = new Shipment();
        shipment.setOrder(savedOrder);
        shipment.setTrackingNumber("TRK-" + savedOrder.getId());
        shipment.setModeOfShipment("Air");
        shipment.setWarehouseBlock("A");
        shipment.setProductImportance("medium");
        shipment.setEstimatedDeliveryAt(LocalDateTime.now().plusDays(3));
        shipmentRepository.save(shipment);

        return savedOrder;
    }

    public OrderResponse createOrderResponse(User user, CreateOrderRequest request) {
        return toOrderResponse(createOrder(user, request));
    }

    public Order updateOrder(Long id, Order order) {
        Order existingOrder = getOrderById(id);

        existingOrder.setUser(order.getUser());
        existingOrder.setStore(order.getStore());
        existingOrder.setTotalPrice(order.getTotalPrice());
        existingOrder.setStatus(order.getStatus());
        existingOrder.setFulfillmentStatus(order.getFulfillmentStatus());
        existingOrder.setShipmentStatus(order.getShipmentStatus());
        existingOrder.setPaymentMethod(order.getPaymentMethod());
        existingOrder.setUpdatedAt(LocalDateTime.now());

        return orderRepository.save(existingOrder);
    }

    public void deleteOrder(Long id) {
        orderRepository.deleteById(id);
    }

    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUser_Id(userId);
    }

    public List<OrderResponse> getOrderResponsesByUserId(Long userId) {
        return toOrderResponses(orderRepository.findByUser_Id(userId));
    }

    public List<Order> getOrdersByStoreId(Long storeId) {
        return orderRepository.findByStore_Id(storeId);
    }

    private OrderResponse toOrderResponse(Order order) {
        return toOrderResponses(List.of(order)).get(0);
    }

    private List<OrderResponse> toOrderResponses(List<Order> orders) {
        if (orders.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        Map<Long, List<OrderItemResponse>> itemsByOrderId = orderItemRepository.findWithProductByOrderIdIn(orderIds).stream()
                .collect(Collectors.groupingBy(
                        item -> item.getOrder().getId(),
                        Collectors.mapping(
                                item -> new OrderItemResponse(
                                        item.getProduct().getId(),
                                        item.getProduct().getName(),
                                        item.getProduct().getCategory() != null ? item.getProduct().getCategory().getName() : null,
                                        item.getQuantity(),
                                        item.getPrice()
                                ),
                                Collectors.toList()
                        )
                ));

        return orders.stream()
                .map(order -> new OrderResponse(order, itemsByOrderId.getOrDefault(order.getId(), Collections.emptyList())))
                .toList();
    }
}
