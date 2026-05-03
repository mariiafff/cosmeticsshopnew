package com.cosmeticsshop.security.config;

import com.cosmeticsshop.model.CustomerProfile;
import com.cosmeticsshop.model.User;
import com.cosmeticsshop.repository.CustomerProfileRepository;
import com.cosmeticsshop.repository.UserRepository;
import com.cosmeticsshop.security.jwt.JwtUtil;
import com.cosmeticsshop.security.service.CustomUserDetailsService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

@Component
public class GoogleOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtUtil jwtUtil;
    private final String frontendUrl;

    public GoogleOAuth2SuccessHandler(
            UserRepository userRepository,
            CustomerProfileRepository customerProfileRepository,
            PasswordEncoder passwordEncoder,
            CustomUserDetailsService customUserDetailsService,
            JwtUtil jwtUtil,
            @Value("${app.frontend.url:http://127.0.0.1:4200}") String frontendUrl
    ) {
        this.userRepository = userRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.customUserDetailsService = customUserDetailsService;
        this.jwtUtil = jwtUtil;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        String email = normalizeEmail(principal.getAttribute("email"));

        if (email.isBlank()) {
            response.sendRedirect(frontendUrl + "/login?oauthError=true");
            return;
        }

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> createGoogleUser(email, principal));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(user.getEmail());
        String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth/callback")
                .queryParam("token", jwtUtil.generateAccessToken(userDetails))
                .queryParam("refreshToken", jwtUtil.generateRefreshToken(userDetails))
                .queryParam("email", user.getEmail())
                .queryParam("role", user.getRole())
                .build()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }

    private User createGoogleUser(String email, OAuth2User principal) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("GOOGLE_OAUTH2_" + UUID.randomUUID()));
        user.setRole("INDIVIDUAL");
        user.setCity(null);
        User savedUser = userRepository.save(user);

        CustomerProfile customerProfile = new CustomerProfile();
        customerProfile.setUser(savedUser);
        customerProfile.setMembershipType("GOLD");
        customerProfileRepository.save(customerProfile);

        return savedUser;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
