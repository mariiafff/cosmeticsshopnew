package com.cosmeticsshop.security.config;

import com.cosmeticsshop.model.CustomerProfile;
import com.cosmeticsshop.model.User;
import com.cosmeticsshop.repository.CustomerProfileRepository;
import com.cosmeticsshop.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DemoUserInitializer {

    @Bean
    @ConditionalOnProperty(name = "app.demo.user.enabled", havingValue = "true", matchIfMissing = true)
    public CommandLineRunner createDemoUser(
            UserRepository userRepository,
            CustomerProfileRepository customerProfileRepository,
            PasswordEncoder passwordEncoder,
            Environment environment
    ) {
        return args -> {
            if (isProduction(environment)) {
                return;
            }

            User demoUser = userRepository.findByEmail("demo@luime.com").orElseGet(User::new);
            demoUser.setEmail("demo@luime.com");
            demoUser.setPassword(passwordEncoder.encode("Demo12345"));
            demoUser.setRole("INDIVIDUAL");
            demoUser.setCity("Istanbul");
            User savedUser = userRepository.save(demoUser);

            CustomerProfile profile = customerProfileRepository.findByUser_Id(savedUser.getId())
                    .orElseGet(CustomerProfile::new);
            profile.setUser(savedUser);
            profile.setCity("Istanbul");
            profile.setMembershipType("GOLD");
            customerProfileRepository.save(profile);
        };
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
