package com.cosmeticsshop.service;

import com.cosmeticsshop.dto.CheckoutItemRequest;
import com.cosmeticsshop.dto.CheckoutRequest;
import com.cosmeticsshop.dto.CreateOrderRequest;
import com.cosmeticsshop.dto.OrderItemResponse;
import com.cosmeticsshop.dto.OrderResponse;
import com.cosmeticsshop.exception.ResourceNotFoundException;
import com.cosmeticsshop.model.Order;
import com.cosmeticsshop.model.OrderItem;
import com.cosmeticsshop.model.Product;
import com.cosmeticsshop.model.User;
import com.cosmeticsshop.repository.OrderItemRepository;
import com.cosmeticsshop.repository.OrderRepository;
import com.cosmeticsshop.repository.ProductRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    public OrderService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            ProductRepository productRepository
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public List<OrderResponse> getAllOrderResponses() {
        return toOrderResponses(orderRepository.findAll());
    }

    public List<OrderResponse> getRecentOrderResponses(int size) {
        return toOrderResponses(orderRepository.findAll(recentOrdersPage(size)).getContent());
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    @Transactional
    public Order saveOrder(Order order) {
        return orderRepository.save(order);
    }

    @Transactional
    public Order createOrder(User user, CreateOrderRequest request) {
        return createOrder(user, toCheckoutRequest(request), "PLACED");
    }

    public OrderResponse createOrderResponse(User user, CreateOrderRequest request) {
        return toOrderResponse(createOrder(user, request));
    }

    @Transactional
    public OrderResponse checkout(User user, CheckoutRequest request) {
        return toOrderResponse(createOrder(user, request, "PAID"));
    }

    @Transactional
    public Order updateOrder(Long id, Order order) {
        Order existingOrder = getOrderById(id);

        if (order.getUser() != null) {
            existingOrder.setUser(order.getUser());
        }
        if (order.getStore() != null) {
            existingOrder.setStore(order.getStore());
        }
        if (order.getTotalPrice() > 0) {
            existingOrder.setTotalPrice(order.getTotalPrice());
        }
        if (order.getStatus() != null && !order.getStatus().isBlank()) {
            existingOrder.setStatus(order.getStatus());
        }
        if (order.getFulfillmentStatus() != null && !order.getFulfillmentStatus().isBlank()) {
            existingOrder.setFulfillmentStatus(order.getFulfillmentStatus());
        }
        if (order.getShipmentStatus() != null && !order.getShipmentStatus().isBlank()) {
            existingOrder.setShipmentStatus(order.getShipmentStatus());
        }
        if (order.getPaymentMethod() != null && !order.getPaymentMethod().isBlank()) {
            existingOrder.setPaymentMethod(order.getPaymentMethod());
        }
        if (order.getOrderDate() != null) {
            existingOrder.setOrderDate(order.getOrderDate());
        }
        existingOrder.setUpdatedAt(LocalDateTime.now());

        return orderRepository.save(existingOrder);
    }

    @Transactional
    public Order updateOrderForStores(Long id, Order order, List<Long> allowedStoreIds) {
        Order existingOrder = getOrderById(id);
        validateStoreAccess(existingOrder, allowedStoreIds);

        if (order.getTotalPrice() > 0) {
            existingOrder.setTotalPrice(order.getTotalPrice());
        }
        if (order.getStatus() != null && !order.getStatus().isBlank()) {
            existingOrder.setStatus(order.getStatus());
        }
        if (order.getFulfillmentStatus() != null && !order.getFulfillmentStatus().isBlank()) {
            existingOrder.setFulfillmentStatus(order.getFulfillmentStatus());
        }
        if (order.getShipmentStatus() != null && !order.getShipmentStatus().isBlank()) {
            existingOrder.setShipmentStatus(order.getShipmentStatus());
        }
        existingOrder.setUpdatedAt(LocalDateTime.now());

        return orderRepository.save(existingOrder);
    }

    @Transactional
    public void deleteOrder(Long id) {
        orderRepository.deleteById(id);
    }

    @Transactional
    public void deleteOrderForStores(Long id, List<Long> allowedStoreIds) {
        Order existingOrder = getOrderById(id);
        validateStoreAccess(existingOrder, allowedStoreIds);
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

    public List<OrderResponse> getOrderResponsesByStoreIds(List<Long> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Order> orders = orderRepository.findByStore_IdIn(storeIds.stream().distinct().toList(), recentOrdersPage(100));
        return toOrderResponses(orders);
    }

    public List<OrderResponse> getOrderResponsesByStoreIds(List<Long> storeIds, int size) {
        if (storeIds == null || storeIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Order> orders = orderRepository.findByStore_IdIn(storeIds.stream().distinct().toList(), recentOrdersPage(size));
        return toOrderResponses(orders);
    }

    private CheckoutRequest toCheckoutRequest(CreateOrderRequest request) {
        if (request.getProductId() == null || request.getQuantity() == null) {
            throw new IllegalArgumentException("Product and quantity are required.");
        }

        CheckoutItemRequest item = new CheckoutItemRequest();
        item.setProductId(request.getProductId());
        item.setQuantity(request.getQuantity());

        CheckoutRequest checkoutRequest = new CheckoutRequest();
        checkoutRequest.setItems(List.of(item));
        checkoutRequest.setPaymentMethod(request.getPaymentMethod());
        return checkoutRequest;
    }

    private Order createOrder(User user, CheckoutRequest request, String status) {
        List<CheckoutItemRequest> items = normalizeItems(request);
        Map<Long, Product> productsById = loadProducts(items);
        Product firstProduct = productsById.get(items.get(0).getProductId());
        Long resolvedStoreId = items.stream()
                .map(item -> productsById.get(item.getProductId()).getStore())
                .filter(store -> store != null && store.getId() != null)
                .map(store -> store.getId())
                .distinct()
                .reduce((left, right) -> {
                    throw new IllegalArgumentException("Checkout currently supports products from a single store.");
                })
                .orElse(null);

        LocalDateTime now = LocalDateTime.now();
        String reference = "ORD-" + System.currentTimeMillis();
        double totalAmount = items.stream()
                .mapToDouble(item -> productsById.get(item.getProductId()).getPrice() * item.getQuantity())
                .sum();

        Order order = new Order();
        order.setUser(user);
        order.setStore(
                resolvedStoreId == null
                        ? null
                        : items.stream()
                                .map(item -> productsById.get(item.getProductId()).getStore())
                                .filter(store -> store != null && resolvedStoreId.equals(store.getId()))
                                .findFirst()
                                .orElse(firstProduct.getStore())
        );
        order.setSourceOrderId(reference);
        order.setOrderNumber(reference);
        order.setIncrementId(reference);
        order.setPaymentMethod(request.getPaymentMethod() == null || request.getPaymentMethod().isBlank() ? "CARD" : request.getPaymentMethod());
        order.setStatus(status);
        order.setFulfillmentStatus("PENDING");
        order.setSalesChannel("WEB");
        order.setShipServiceLevel("STANDARD");
        order.setTotalPrice(totalAmount);
        order.setCurrencyCode("USD");
        order.setNormalizedGrandTotalUsd(totalAmount);
        order.setOrderDate(now);
        order.setCreatedAt(now);
        order.setShipmentStatus("PROCESSING");

        Order savedOrder = orderRepository.save(order);

        List<OrderItem> orderItems = new ArrayList<>();
        for (CheckoutItemRequest item : items) {
            Product product = productsById.get(item.getProductId());
            double unitPrice = product.getPrice();

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProduct(product);
            orderItem.setQuantity(item.getQuantity());
            orderItem.setPrice(unitPrice);
            orderItems.add(orderItem);
        }
        orderItemRepository.saveAll(orderItems);

        return savedOrder;
    }

    private List<CheckoutItemRequest> normalizeItems(CheckoutRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cart items are required.");
        }

        List<CheckoutItemRequest> normalized = new ArrayList<>();
        for (CheckoutItemRequest item : request.getItems()) {
            if (item.getProductId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Each cart item must include a productId and positive quantity.");
            }
            normalized.add(item);
        }
        return normalized;
    }

    private Map<Long, Product> loadProducts(List<CheckoutItemRequest> items) {
        List<Long> productIds = items.stream()
                .map(CheckoutItemRequest::getProductId)
                .distinct()
                .toList();

        Map<Long, Product> productsById = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, product -> product, (left, right) -> left, LinkedHashMap::new));

        for (Long productId : productIds) {
            if (!productsById.containsKey(productId)) {
                throw new ResourceNotFoundException("Product not found: " + productId);
            }
        }

        return productsById;
    }

    private OrderResponse toOrderResponse(Order order) {
        return toOrderResponses(List.of(order)).get(0);
    }

    private void validateStoreAccess(Order order, List<Long> allowedStoreIds) {
        if (allowedStoreIds == null || allowedStoreIds.isEmpty()) {
            throw new ResourceNotFoundException("You do not have a store assigned yet.");
        }

        Long orderStoreId = order.getStore() == null ? null : order.getStore().getId();
        if (orderStoreId == null || !Set.copyOf(allowedStoreIds).contains(orderStoreId)) {
            throw new ResourceNotFoundException("You can only manage orders for your own store.");
        }
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

    private PageRequest recentOrdersPage(int size) {
        int normalizedSize = Math.min(Math.max(size, 1), 250);
        return PageRequest.of(0, normalizedSize, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));
    }
}
