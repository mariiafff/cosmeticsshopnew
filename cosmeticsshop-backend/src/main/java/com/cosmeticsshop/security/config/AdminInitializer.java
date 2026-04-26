package com.cosmeticsshop.security.config;

import com.cosmeticsshop.model.Category;
import com.cosmeticsshop.model.CustomerProfile;
import com.cosmeticsshop.model.Order;
import com.cosmeticsshop.model.OrderItem;
import com.cosmeticsshop.model.Product;
import com.cosmeticsshop.model.Review;
import com.cosmeticsshop.model.Shipment;
import com.cosmeticsshop.model.Store;
import com.cosmeticsshop.model.User;
import com.cosmeticsshop.repository.CategoryRepository;
import com.cosmeticsshop.repository.CustomerProfileRepository;
import com.cosmeticsshop.repository.OrderItemRepository;
import com.cosmeticsshop.repository.OrderRepository;
import com.cosmeticsshop.repository.ProductRepository;
import com.cosmeticsshop.repository.ReviewRepository;
import com.cosmeticsshop.repository.ShipmentRepository;
import com.cosmeticsshop.repository.StoreRepository;
import com.cosmeticsshop.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Configuration
public class AdminInitializer {

    @Bean
    @ConditionalOnProperty(name = "app.admin.seed.enabled", havingValue = "true", matchIfMissing = true)
    public CommandLineRunner createDefaultAdmin(
            UserRepository userRepository,
            StoreRepository storeRepository,
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            ShipmentRepository shipmentRepository,
            ReviewRepository reviewRepository,
            CustomerProfileRepository customerProfileRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email}") String adminEmail,
            @Value("${app.admin.password}") String adminPassword
    ) {
        return args -> {
            userRepository.findByEmail(adminEmail).ifPresentOrElse(admin -> {
                admin.setPassword(passwordEncoder.encode(adminPassword));
                admin.setRole("ADMIN");
                userRepository.save(admin);
            }, () -> {
                User admin = new User();
                admin.setEmail(adminEmail);
                admin.setPassword(passwordEncoder.encode(adminPassword));
                admin.setRole("ADMIN");
                admin.setFirstName("Platform");
                admin.setLastName("Admin");
                userRepository.save(admin);
            });

            if (storeRepository.count() > 0 || productRepository.count() > 0) {
                Long storeId = storeRepository.findAll().stream()
                        .findFirst()
                        .map(Store::getId)
                        .orElse(null);

                userRepository.findByEmail("manager@luna-beauty.com").ifPresentOrElse(corporate -> {
                    corporate.setPassword(passwordEncoder.encode("Manager123!"));
                    corporate.setRole("CORPORATE");
                    corporate.setStoreId(storeId);
                    userRepository.save(corporate);
                }, () -> {
                    User corporate = new User();
                    corporate.setEmail("manager@luna-beauty.com");
                    corporate.setPassword(passwordEncoder.encode("Manager123!"));
                    corporate.setRole("CORPORATE");
                    corporate.setFirstName("Luna");
                    corporate.setLastName("Manager");
                    corporate.setStoreId(storeId);
                    corporate.setCity("Istanbul");
                    userRepository.save(corporate);
                });
                userRepository.findByEmail("shopper@example.com").ifPresentOrElse(shopper -> {
                    shopper.setPassword(passwordEncoder.encode("Shopper123!"));
                    shopper.setRole("INDIVIDUAL");
                    userRepository.save(shopper);
                }, () -> {
                    User shopper = new User();
                    shopper.setEmail("shopper@example.com");
                    shopper.setPassword(passwordEncoder.encode("Shopper123!"));
                    shopper.setRole("INDIVIDUAL");
                    shopper.setFirstName("Ayse");
                    shopper.setLastName("Shopper");
                    shopper.setCity("Istanbul");
                    shopper.setMembershipType("GOLD");
                    userRepository.save(shopper);
                });
                return;
            }

            Category skincare = new Category();
            skincare.setName("Skincare");
            skincare.setDescription("Daily skincare essentials");
            Category savedCategory = categoryRepository.save(skincare);

            Store store = new Store();
            store.setName("Luna Beauty");
            store.setCity("Istanbul");
            store.setCountry("Turkey");
            store.setStatus("OPEN");
            store.setDescription("Corporate beauty store for analytics demos.");
            Store savedStore = storeRepository.save(store);

            User corporate = new User();
            corporate.setEmail("manager@luna-beauty.com");
            corporate.setPassword(passwordEncoder.encode("Manager123!"));
            corporate.setRole("CORPORATE");
            corporate.setFirstName("Luna");
            corporate.setLastName("Manager");
            corporate.setStoreId(savedStore.getId());
            corporate.setCity("Istanbul");
            userRepository.save(corporate);

            savedStore.setOwnerUserId(corporate.getId());
            storeRepository.save(savedStore);

            User shopper = new User();
            shopper.setEmail("shopper@example.com");
            shopper.setPassword(passwordEncoder.encode("Shopper123!"));
            shopper.setRole("INDIVIDUAL");
            shopper.setFirstName("Ayse");
            shopper.setLastName("Shopper");
            shopper.setCity("Istanbul");
            shopper.setMembershipType("GOLD");
            userRepository.save(shopper);

            CustomerProfile profile = new CustomerProfile();
            profile.setUser(shopper);
            profile.setAge(27);
            profile.setGender("Female");
            profile.setCity("Istanbul");
            profile.setMembershipType("GOLD");
            profile.setPreferredCategory("Skincare");
            customerProfileRepository.save(profile);

            Product serum = new Product();
            serum.setName("Vitamin C Glow Serum");
            serum.setDescription("Brightening serum with vitamin C.");
            serum.setPrice(34.90);
            serum.setStore(savedStore);
            serum.setSellerId(corporate.getId());
            serum.setSku("SKU-GLOW-001");
            serum.setCategory(savedCategory);
            serum.setStockQuantity(24);
            Product savedProduct = productRepository.save(serum);

            Order order = new Order();
            order.setUser(shopper);
            order.setStore(savedStore);
            order.setOrderNumber("ORD-SEED-001");
            order.setPaymentMethod("CARD");
            order.setStatus("DELIVERED");
            order.setFulfillmentStatus("COMPLETED");
            order.setShipmentStatus("DELIVERED");
            order.setSalesChannel("WEB");
            order.setShipServiceLevel("EXPRESS");
            order.setCreatedAt(LocalDateTime.now().minusDays(10));
            order.setUpdatedAt(LocalDateTime.now().minusDays(7));
            order.setTotalPrice(69.80);
            Order savedOrder = orderRepository.save(order);

            OrderItem item = new OrderItem();
            item.setOrder(savedOrder);
            item.setProduct(savedProduct);
            item.setQuantity(2);
            item.setPrice(savedProduct.getPrice());
            orderItemRepository.save(item);

            Shipment shipment = new Shipment();
            shipment.setOrder(savedOrder);
            shipment.setWarehouseBlock("A");
            shipment.setModeOfShipment("Air");
            shipment.setTrackingNumber("TRK-SEED-001");
            shipment.setStatus("DELIVERED");
            shipment.setEstimatedDeliveryAt(LocalDateTime.now().minusDays(8));
            shipment.setDeliveredAt(LocalDateTime.now().minusDays(7));
            shipmentRepository.save(shipment);

            Review review = new Review();
            review.setProduct(savedProduct);
            review.setUser(shopper);
            review.setRating(5);
            review.setTitle("Great daily serum");
            review.setComment("Skin feels brighter and lightweight after a week.");
            review.setHelpfulVotes(8);
            review.setTotalVotes(10);
            reviewRepository.save(review);
        };
    }
}
