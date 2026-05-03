package com.cosmeticsshop.security.config;

import com.cosmeticsshop.model.CustomerProfile;
import com.cosmeticsshop.model.Order;
import com.cosmeticsshop.model.OrderItem;
import com.cosmeticsshop.model.Product;
import com.cosmeticsshop.model.Review;
import com.cosmeticsshop.model.Store;
import com.cosmeticsshop.model.User;
import com.cosmeticsshop.repository.CategoryRepository;
import com.cosmeticsshop.repository.CustomerProfileRepository;
import com.cosmeticsshop.repository.OrderItemRepository;
import com.cosmeticsshop.repository.OrderRepository;
import com.cosmeticsshop.repository.ProductRepository;
import com.cosmeticsshop.repository.ReviewRepository;
import com.cosmeticsshop.repository.StoreRepository;
import com.cosmeticsshop.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Configuration
public class DemoAccountsInitializer {

    private static final Logger log = LoggerFactory.getLogger(DemoAccountsInitializer.class);
    private static final String SELLER_DEMO_STORE_NAME = "Luna Marketplace";

    @Bean
    @org.springframework.core.annotation.Order(20)
    @ConditionalOnProperty(name = "app.demo.accounts.enabled", havingValue = "true")
    public CommandLineRunner createDemoAccounts(
            UserRepository userRepository,
            CustomerProfileRepository customerProfileRepository,
            StoreRepository storeRepository,
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            ReviewRepository reviewRepository,
            PasswordEncoder passwordEncoder,
            JdbcTemplate jdbcTemplate,
            Environment environment) {
        return args -> {
            if (isProduction(environment)) {
                return;
            }

            User individual = upsertUser(
                    userRepository,
                    passwordEncoder,
                    "demo@luime.com",
                    "Demo12345",
                    "INDIVIDUAL",
                    "Istanbul");
            upsertCustomerProfile(customerProfileRepository, individual, "Istanbul", "GOLD");

            User corporate = upsertUser(
                    userRepository,
                    passwordEncoder,
                    "seller@test.com",
                    "Seller123!",
                    "CORPORATE",
                    "Istanbul");
            Store sellerStore = upsertCorporateStore(storeRepository, productRepository, corporate);
            seedCorporateSalesDemo(
                    userRepository,
                    customerProfileRepository,
                    storeRepository,
                    categoryRepository,
                    productRepository,
                    orderRepository,
                    orderItemRepository,
                    reviewRepository,
                    individual,
                    corporate,
                    passwordEncoder
            );
            logSellerDiagnostics(jdbcTemplate, corporate, sellerStore);

            upsertUser(
                    userRepository,
                    passwordEncoder,
                    "admin@cosmeticsshop.com",
                    "Admin123!",
                    "ADMIN",
                    "Istanbul");

            seedCategoriesIfEmpty(categoryRepository);
        };
    }

    private User upsertUser(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            String email,
            String rawPassword,
            String role,
            String city) {
        User user = userRepository.findByEmail(email).orElseGet(User::new);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setCity(city);
        return userRepository.save(user);
    }

    private void upsertCustomerProfile(
            CustomerProfileRepository customerProfileRepository,
            User user,
            String city,
            String membershipType) {
        CustomerProfile profile = customerProfileRepository.findByUser_Id(user.getId())
                .orElseGet(CustomerProfile::new);
        profile.setUser(user);
        profile.setCity(city);
        profile.setMembershipType(membershipType);
        customerProfileRepository.save(profile);
    }

    private Store upsertCorporateStore(
            StoreRepository storeRepository,
            ProductRepository productRepository,
            User corporateUser
    ) {
        if (corporateUser == null || corporateUser.getId() == null) {
            throw new IllegalStateException("Cannot create or migrate seller demo store without a valid owner user id.");
        }

        List<Store> existingStores = storeRepository.findAllByOwnerUserIdAndNameIgnoreCase(
                corporateUser.getId(),
                SELLER_DEMO_STORE_NAME);

        Store store;

        if (!existingStores.isEmpty()) {
            store = existingStores.get(0);
        } else {
            store = storeRepository.findAll().stream()
                    .filter(this::isLunaDemoStore)
                    .findFirst()
                    .orElseGet(Store::new);
        }

        store.setOwnerUserId(corporateUser.getId());
        store.setName(SELLER_DEMO_STORE_NAME);
        store.setStatus("OPEN");
        store.setCity("Istanbul");
        store.setCountry("Turkey");
        store.setDescription("Seeded corporate demo store for product and order management.");

        Store savedStore = saveStoreWithOwnerGuard(storeRepository, store);
        consolidateSellerDemoStores(storeRepository, productRepository, corporateUser, savedStore);
        return savedStore;
    }

    private void seedCategoriesIfEmpty(CategoryRepository categoryRepository) {
        if (categoryRepository.count() > 0) {
            return;
        }

        categoryRepository.save(createCategory("Electronics"));
        categoryRepository.save(createCategory("Home & Living"));
        categoryRepository.save(createCategory("Fashion"));
        categoryRepository.save(createCategory("Accessories"));
        categoryRepository.save(createCategory("Office"));
        categoryRepository.save(createCategory("Lifestyle"));
    }

    private void seedCorporateSalesDemo(
            UserRepository userRepository,
            CustomerProfileRepository customerProfileRepository,
            StoreRepository storeRepository,
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            ReviewRepository reviewRepository,
            User demoCustomer,
            User primaryCorporate,
            PasswordEncoder passwordEncoder
    ) {
        seedManagerStoreSalesDemo(
                customerProfileRepository,
                storeRepository,
                categoryRepository,
                productRepository,
                orderRepository,
                orderItemRepository,
                primaryCorporate,
                demoCustomer
        );

        Store savedStore = upsertCorporateStore(storeRepository, productRepository, primaryCorporate);

        com.cosmeticsshop.model.Category category = categoryRepository.findAll().stream()
                .filter(existing -> "Skincare".equalsIgnoreCase(existing.getName()))
                .findFirst()
                .orElseGet(() -> categoryRepository.save(createCategory("Skincare")));

        Product serum = upsertDemoProduct(productRepository, savedStore, category, "AI-DEMO-SERUM", "Luna Vitamin C Serum", 34.90, 35);
        Product cream = upsertDemoProduct(productRepository, savedStore, category, "AI-DEMO-CREAM", "Luna Barrier Repair Cream", 42.50, 28);
        Product cleanser = upsertDemoProduct(productRepository, savedStore, category, "AI-DEMO-CLEANSER", "Luna Gentle Gel Cleanser", 21.00, 50);
        Product toner = upsertDemoProduct(productRepository, savedStore, category, "AI-DEMO-TONER", "Luna Rose Hydrating Toner", 18.75, 44);
        Product mask = upsertDemoProduct(productRepository, savedStore, category, "AI-DEMO-MASK", "Luna Overnight Glow Mask", 29.95, 30);

        User shopper = upsertUser(
                userRepository,
                passwordEncoder,
                "ai-shopper@luime.com",
                "Shopper123!",
                "INDIVIDUAL",
                "Istanbul"
        );
        upsertCustomerProfile(customerProfileRepository, shopper, "Istanbul", "GOLD");

        Order order = orderRepository.findByOrderNumber("ORD-AI-DEMO-CURRENT-MONTH")
                .orElseGet(Order::new);
        order.setUser(shopper);
        order.setStore(savedStore);
        order.setOrderNumber("ORD-AI-DEMO-CURRENT-MONTH");
        order.setIncrementId("ORD-AI-DEMO-CURRENT-MONTH");
        order.setSourceOrderId("ORD-AI-DEMO-CURRENT-MONTH");
        order.setPaymentMethod("CARD");
        order.setStatus("DELIVERED");
        order.setFulfillmentStatus("COMPLETED");
        order.setShipmentStatus("DELIVERED");
        order.setSalesChannel("WEB");
        order.setShipServiceLevel("STANDARD");
        LocalDateTime orderDate = LocalDateTime.now().minusDays(14);
        order.setCreatedAt(orderDate);
        order.setOrderDate(orderDate);
        order.setUpdatedAt(orderDate.plusDays(1));
        order.setTotalPrice(
                serum.getPrice() * 12
                        + cream.getPrice() * 9
                        + cleanser.getPrice() * 7
                        + toner.getPrice() * 5
                        + mask.getPrice() * 3
        );
        order.setCurrencyCode("USD");
        order.setNormalizedGrandTotalUsd(order.getTotalPrice());
        Order savedOrder = orderRepository.save(order);

        if (orderItemRepository.findWithProductByOrderIdIn(List.of(savedOrder.getId())).isEmpty()) {
            orderItemRepository.save(new OrderItem(12, serum.getPrice(), savedOrder, serum));
            orderItemRepository.save(new OrderItem(9, cream.getPrice(), savedOrder, cream));
            orderItemRepository.save(new OrderItem(7, cleanser.getPrice(), savedOrder, cleanser));
            orderItemRepository.save(new OrderItem(5, toner.getPrice(), savedOrder, toner));
            orderItemRepository.save(new OrderItem(3, mask.getPrice(), savedOrder, mask));
        }

        seedDemoCustomerCreditCardOrder(
                orderRepository,
                orderItemRepository,
                demoCustomer,
                savedStore,
                serum,
                cream,
                cleanser
        );

        seedReviewAnalyticsDemo(
                reviewRepository,
                List.of(serum, cream, cleanser, toner, mask),
                List.of(demoCustomer, shopper)
        );
    }

    private void seedManagerStoreSalesDemo(
            CustomerProfileRepository customerProfileRepository,
            StoreRepository storeRepository,
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            User corporate,
            User demoCustomer
    ) {
        Store savedStore = upsertCorporateStore(storeRepository, productRepository, corporate);

        com.cosmeticsshop.model.Category category = categoryRepository.findAll().stream()
                .filter(existing -> "Skincare".equalsIgnoreCase(existing.getName()))
                .findFirst()
                .orElseGet(() -> categoryRepository.save(createCategory("Skincare")));

        Product serum = upsertDemoProduct(productRepository, savedStore, category, "AI-MANAGER-SERUM", "Luna Manager Bright Serum", 31.90, 40);
        Product cream = upsertDemoProduct(productRepository, savedStore, category, "AI-MANAGER-CREAM", "Luna Manager Repair Cream", 39.50, 24);
        upsertCustomerProfile(customerProfileRepository, demoCustomer, "Istanbul", "GOLD");

        Order order = orderRepository.findByOrderNumber("ORD-AI-MANAGER-LATEST")
                .orElseGet(Order::new);
        order.setUser(demoCustomer);
        order.setStore(savedStore);
        order.setOrderNumber("ORD-AI-MANAGER-LATEST");
        order.setIncrementId("ORD-AI-MANAGER-LATEST");
        order.setSourceOrderId("ORD-AI-MANAGER-LATEST");
        order.setPaymentMethod("CARD");
        order.setStatus("DELIVERED");
        order.setFulfillmentStatus("COMPLETED");
        order.setShipmentStatus("DELIVERED");
        order.setSalesChannel("WEB");
        order.setShipServiceLevel("STANDARD");
        LocalDateTime orderDate = LocalDateTime.now().minusDays(11);
        order.setCreatedAt(orderDate);
        order.setOrderDate(orderDate);
        order.setUpdatedAt(orderDate);
        order.setTotalPrice(serum.getPrice() * 2 + cream.getPrice());
        order.setCurrencyCode("USD");
        order.setNormalizedGrandTotalUsd(order.getTotalPrice());
        Order savedOrder = orderRepository.save(order);

        if (orderItemRepository.findWithProductByOrderIdIn(List.of(savedOrder.getId())).isEmpty()) {
            orderItemRepository.save(new OrderItem(2, serum.getPrice(), savedOrder, serum));
            orderItemRepository.save(new OrderItem(1, cream.getPrice(), savedOrder, cream));
        }
    }

    private void seedDemoCustomerCreditCardOrder(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            User demoCustomer,
            Store store,
            Product serum,
            Product cream,
            Product cleanser
    ) {
        Order order = orderRepository.findByOrderNumber("ORD-DEMO-CREDIT-CARD-LATEST")
                .orElseGet(Order::new);
        order.setUser(demoCustomer);
        order.setStore(store);
        order.setOrderNumber("ORD-DEMO-CREDIT-CARD-LATEST");
        order.setIncrementId("ORD-DEMO-CREDIT-CARD-LATEST");
        order.setSourceOrderId("ORD-DEMO-CREDIT-CARD-LATEST");
        order.setPaymentMethod("credit_card");
        order.setStatus("DELIVERED");
        order.setFulfillmentStatus("COMPLETED");
        order.setShipmentStatus("DELIVERED");
        order.setSalesChannel("WEB");
        order.setShipServiceLevel("STANDARD");
        LocalDateTime orderDate = LocalDateTime.now().minusDays(10);
        order.setCreatedAt(orderDate);
        order.setOrderDate(orderDate);
        order.setUpdatedAt(orderDate.plusDays(1));
        order.setTotalPrice(serum.getPrice() * 1 + cream.getPrice() * 2 + cleanser.getPrice() * 1);
        order.setCurrencyCode("USD");
        order.setNormalizedGrandTotalUsd(order.getTotalPrice());
        Order savedOrder = orderRepository.save(order);

        if (orderItemRepository.findWithProductByOrderIdIn(List.of(savedOrder.getId())).isEmpty()) {
            orderItemRepository.save(new OrderItem(1, serum.getPrice(), savedOrder, serum));
            orderItemRepository.save(new OrderItem(2, cream.getPrice(), savedOrder, cream));
            orderItemRepository.save(new OrderItem(1, cleanser.getPrice(), savedOrder, cleanser));
        }
    }

    private Product upsertDemoProduct(
            ProductRepository productRepository,
            Store store,
            com.cosmeticsshop.model.Category category,
            String sku,
            String name,
            double price,
            int stockQuantity
    ) {
        Product product = productRepository.findBySku(sku).orElseGet(Product::new);
        product.setStore(store);
        product.setCategory(category);
        product.setSku(sku);
        product.setStockCode(sku);
        product.setName(name);
        product.setDescription("Seeded product for current-month AI sales analytics.");
        product.setPrice(price);
        product.setStockQuantity(stockQuantity);
        product.setStatus("ACTIVE");
        product.setCurrencyCode("USD");
        product.setUnitPrice(java.math.BigDecimal.valueOf(price));
        product.setNormalizedUnitPriceUsd(java.math.BigDecimal.valueOf(price));
        product.setProductImportance("HIGH");
        LocalDateTime now = LocalDateTime.now();
        if (product.getCreatedAt() == null) {
            product.setCreatedAt(now);
        }
        product.setUpdatedAt(now);
        return productRepository.save(product);
    }

    private void seedReviewAnalyticsDemo(
            ReviewRepository reviewRepository,
            List<Product> products,
            List<User> customers
    ) {
        if (reviewRepository.count() > 0 || products.size() < 5 || customers.isEmpty()) {
            return;
        }

        seedReview(reviewRepository, products.get(0), customers.get(0), 5, "Excellent serum with a visible glow.");
        seedReview(reviewRepository, products.get(0), customers.get(1 % customers.size()), 5, "Lightweight and brightening.");
        seedReview(reviewRepository, products.get(0), customers.get(0), 4, "Very good daily skincare product.");

        seedReview(reviewRepository, products.get(1), customers.get(0), 5, "Rich texture and strong barrier support.");
        seedReview(reviewRepository, products.get(1), customers.get(1 % customers.size()), 4, "Comfortable cream for daily use.");

        seedReview(reviewRepository, products.get(2), customers.get(0), 4, "Gentle cleanser, does not dry my skin.");
        seedReview(reviewRepository, products.get(2), customers.get(1 % customers.size()), 4, "Good cleanser for morning routine.");
        seedReview(reviewRepository, products.get(2), customers.get(0), 5, "Fresh and effective cleanser.");

        seedReview(reviewRepository, products.get(3), customers.get(1 % customers.size()), 3, "Nice toner, but average hydration.");
        seedReview(reviewRepository, products.get(4), customers.get(0), 5, "Great overnight mask.");
    }

    private void seedReview(
            ReviewRepository reviewRepository,
            Product product,
            User customer,
            int rating,
            String comment
    ) {
        Review review = new Review();
        review.setProduct(product);
        review.setUser(customer);
        review.setRating(rating);
        review.setComment(comment);
        review.setHelpfulVotes(Math.max(1, rating));
        review.setTotalVotes(Math.max(1, rating + 1));
        reviewRepository.save(review);
    }

    private com.cosmeticsshop.model.Category createCategory(String name) {
        com.cosmeticsshop.model.Category category = new com.cosmeticsshop.model.Category();
        category.setName(name);
        category.setDescription(name + " products");
        return category;
    }

    private void consolidateSellerDemoStores(
            StoreRepository storeRepository,
            ProductRepository productRepository,
            User seller,
            Store primaryStore
    ) {
        List<Store> duplicateDemoStores = storeRepository.findAll().stream()
                .filter(store -> store.getId() != null && primaryStore.getId() != null)
                .filter(store -> !Objects.equals(store.getId(), primaryStore.getId()))
                .filter(this::isLunaDemoStore)
                .toList();

        for (Store duplicateStore : duplicateDemoStores) {
            List<Product> products = productRepository.findByStore_Id(duplicateStore.getId());
            for (Product product : products) {
                product.setStore(primaryStore);
            }
            productRepository.saveAll(products);

            duplicateStore.setOwnerUserId(seller.getId());
            duplicateStore.setStatus("CLOSED");
            saveStoreWithOwnerGuard(storeRepository, duplicateStore);
        }
    }

    private Store saveStoreWithOwnerGuard(StoreRepository storeRepository, Store store) {
        if (store == null) {
            throw new IllegalStateException("Cannot save a null store.");
        }
        if (store.getOwnerUserId() == null) {
            throw new IllegalStateException("Cannot save store with null owner_id. storeId=" + store.getId());
        }
        log.info("seller_demo saving store id={} ownerId={}", store.getId(), store.getOwnerUserId());
        return storeRepository.save(store);
    }

    private boolean isLunaDemoStore(Store store) {
        if (store == null || store.getName() == null) {
            return false;
        }
        return SELLER_DEMO_STORE_NAME.equalsIgnoreCase(store.getName())
                || "Luna Beauty".equalsIgnoreCase(store.getName())
                || "Luna Sales Demo Store".equalsIgnoreCase(store.getName());
    }

    private void logSellerDiagnostics(JdbcTemplate jdbcTemplate, User seller, Store sellerStore) {
        Long storeId = sellerStore == null ? null : sellerStore.getId();
        if (seller == null || seller.getId() == null || storeId == null) {
            log.warn("seller_demo diagnostics skipped sellerId={} storeId={}", seller == null ? null : seller.getId(), storeId);
            return;
        }

        Long productCount = jdbcTemplate.queryForObject(
                "select count(*) from products where store_id = ?",
                Long.class,
                storeId
        );
        Long orderItemCount = jdbcTemplate.queryForObject("""
                select count(*)
                from order_items oi
                join products p on p.id = oi.product_id
                where p.store_id = ?
                """, Long.class, storeId);
        Long orderCount = jdbcTemplate.queryForObject("""
                select count(distinct o.id)
                from orders o
                join order_items oi on oi.order_id = o.id
                join products p on p.id = oi.product_id
                where p.store_id = ?
                """, Long.class, storeId);
        LocalDateTime latestOrderDate = jdbcTemplate.queryForObject("""
                select max(coalesce(o.order_date, o.created_at))
                from orders o
                join order_items oi on oi.order_id = o.id
                join products p on p.id = oi.product_id
                where p.store_id = ?
                """, LocalDateTime.class, storeId);
        List<Map<String, Object>> recentRows = jdbcTemplate.queryForList("""
                select p.name as product_name, o.order_date, oi.quantity, oi.unit_price
                from order_items oi
                join products p on p.id = oi.product_id
                join orders o on o.id = oi.order_id
                where p.store_id = ?
                order by o.order_date desc
                limit 5
                """, storeId);

        log.info(
                "seller_demo diagnostics userSql=\"SELECT id, email, role_type FROM users WHERE email = 'seller@test.com'\" userId={} email={} role={} storeSql=\"SELECT id, name, owner_id FROM stores WHERE owner_id = ?\" storeId={} products={} orderItems={} orders={} latestOrderDate={} recentSoldRows={}",
                seller.getId(),
                seller.getEmail(),
                seller.getRole(),
                storeId,
                productCount,
                orderItemCount,
                orderCount,
                latestOrderDate,
                recentRows
        );
    }

    private boolean isProduction(Environment environment) {
        for (String profile : environment.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }
}
