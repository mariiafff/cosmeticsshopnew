package com.cosmeticsshop.controller;

import com.cosmeticsshop.dto.CheckoutRequest;
import com.cosmeticsshop.dto.CreateOrderRequest;
import com.cosmeticsshop.dto.OrderResponse;
import com.cosmeticsshop.model.Order;
import com.cosmeticsshop.model.User;
import com.cosmeticsshop.repository.StoreRepository;
import com.cosmeticsshop.repository.UserRepository;
import com.cosmeticsshop.service.OrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;

    public OrderController(OrderService orderService, UserRepository userRepository, StoreRepository storeRepository) {
        this.orderService = orderService;
        this.userRepository = userRepository;
        this.storeRepository = storeRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CORPORATE')")
    public List<OrderResponse> getAllOrders(@RequestParam(defaultValue = "100") int size) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElseThrow();

        if ("CORPORATE".equalsIgnoreCase(user.getRole())) {
            List<Long> storeIds = storeRepository.findByOwnerUserId(user.getId()).stream()
                    .map(store -> store.getId())
                    .toList();
            return orderService.getOrderResponsesByStoreIds(storeIds, size);
        }

        return orderService.getRecentOrderResponses(size);
    }

    @PostMapping
    @PreAuthorize("hasRole('INDIVIDUAL')")
    public OrderResponse createOrder(@RequestBody CreateOrderRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElseThrow();
        return orderService.createOrderResponse(user, request);
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasRole('INDIVIDUAL')")
    public OrderResponse checkout(@RequestBody CheckoutRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElseThrow();
        return orderService.checkout(user, request);
    }

    @GetMapping("/{id}")
    public Order getOrderById(@PathVariable Long id) {
        return orderService.getOrderById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CORPORATE')")
    public Order updateOrder(@PathVariable Long id, @RequestBody Order order) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElseThrow();

        if ("CORPORATE".equalsIgnoreCase(user.getRole())) {
            List<Long> storeIds = storeRepository.findByOwnerUserId(user.getId()).stream()
                    .map(store -> store.getId())
                    .toList();
            return orderService.updateOrderForStores(id, order, storeIds);
        }
        return orderService.updateOrder(id, order);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('INDIVIDUAL')")
    public List<OrderResponse> getMyOrders() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElseThrow();
        return orderService.getOrderResponsesByUserId(user.getId());
    }
}
