package com.cosmeticsshop.security.config;

import com.cosmeticsshop.model.CustomerProfile;
import com.cosmeticsshop.model.Store;
import com.cosmeticsshop.model.User;
import com.cosmeticsshop.repository.CategoryRepository;
import com.cosmeticsshop.repository.CustomerProfileRepository;
import com.cosmeticsshop.repository.StoreRepository;
import com.cosmeticsshop.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Configuration
public class DemoAccountsInitializer {

    @Bean
    @ConditionalOnProperty(name = "app.demo.accounts.enabled", havingValue = "true")
    public CommandLineRunner createDemoAccounts(
            UserRepository userRepository,
            CustomerProfileRepository customerProfileRepository,
            StoreRepository storeRepository,
            CategoryRepository categoryRepository,
            PasswordEncoder passwordEncoder,
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
                    "manager@luna-beauty.com",
                    "Manager123!",
                    "CORPORATE",
                    "Istanbul");
            upsertCorporateStore(storeRepository, corporate);

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

    private void upsertCorporateStore(StoreRepository storeRepository, User corporateUser) {
        List<Store> existingStores = storeRepository.findAllByOwnerUserIdAndNameIgnoreCase(
                corporateUser.getId(),
                "Luna Marketplace");

        Store store;

        if (!existingStores.isEmpty()) {
            store = existingStores.get(0);
        } else {
            store = new Store();
        }

        store.setOwnerUserId(corporateUser.getId());
        store.setName("Luna Marketplace");
        store.setStatus("OPEN");
        store.setCity("Istanbul");
        store.setCountry("Turkey");
        store.setDescription("Seeded corporate demo store for product and order management.");

        storeRepository.save(store);
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

    private com.cosmeticsshop.model.Category createCategory(String name) {
        com.cosmeticsshop.model.Category category = new com.cosmeticsshop.model.Category();
        category.setName(name);
        category.setDescription(name + " products");
        return category;
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