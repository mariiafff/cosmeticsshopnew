package com.cosmeticsshop.security.controller;

import com.cosmeticsshop.dto.RefreshTokenRequest;
import com.cosmeticsshop.model.CustomerProfile;
import com.cosmeticsshop.model.User;
import com.cosmeticsshop.repository.CustomerProfileRepository;
import com.cosmeticsshop.repository.UserRepository;
import com.cosmeticsshop.security.dto.AuthResponse;
import com.cosmeticsshop.security.dto.LoginRequest;
import com.cosmeticsshop.security.dto.RegisterRequest;
import com.cosmeticsshop.security.jwt.JwtUtil;
import com.cosmeticsshop.security.service.CustomUserDetailsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService customUserDetailsService;
    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(
            AuthenticationManager authenticationManager,
            CustomUserDetailsService customUserDetailsService,
            UserRepository userRepository,
            CustomerProfileRepository customerProfileRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil
    ) {
        this.authenticationManager = authenticationManager;
        this.customUserDetailsService = customUserDetailsService;
        this.userRepository = userRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtUtil.generateAccessToken(userDetails);
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        return ResponseEntity.ok(new AuthResponse(token, refreshToken, user.getEmail(), user.getRole()));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required.");
        }
        if (request.getPassword() == null || request.getPassword().trim().length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 6 characters.");
        }
        if (isBlank(request.getFirstName()) || isBlank(request.getLastName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "First name and last name are required.");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists. Please login or use another email.");
        }

        User user = new User();
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setEmail(email);
        user.setRole(normalizeRole(request.getRole(), request.getAccountType()));
        user.setPassword(passwordEncoder.encode(request.getPassword().trim()));
        user.setCity(normalizeNullable(request.getCity()));
        User savedUser = userRepository.save(user);

        if ("INDIVIDUAL".equals(savedUser.getRole())) {
            CustomerProfile customerProfile = customerProfileRepository.findByUser_Id(savedUser.getId())
                    .orElseGet(CustomerProfile::new);
            customerProfile.setUser(savedUser);
            customerProfile.setCity(savedUser.getCity());
            customerProfile.setMembershipType(normalizeMembership(request));
            customerProfileRepository.save(customerProfile);
        }

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(savedUser.getEmail());
        String token = jwtUtil.generateAccessToken(userDetails);
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(token, refreshToken, savedUser.getEmail(), savedUser.getRole()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshTokenRequest request) {
        jwtUtil.validateRefreshToken(request.getRefreshToken());
        String email = jwtUtil.extractEmail(request.getRefreshToken());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);
        return ResponseEntity.ok(new AuthResponse(
                jwtUtil.generateAccessToken(userDetails),
                jwtUtil.generateRefreshToken(userDetails),
                user.getEmail(),
                user.getRole()
        ));
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isBlank();
    }

    private String normalizeNullable(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private String normalizeMembership(RegisterRequest request) {
        String membershipType = !isBlank(request.getMembershipType())
                ? request.getMembershipType()
                : request.getMembership();

        if (isBlank(membershipType)) {
            return "GOLD";
        }

        String normalized = membershipType.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "GOLD", "SILVER", "BRONZE" -> normalized;
            default -> "GOLD";
        };
    }

    private String normalizeRole(String role, String accountType) {
        String normalized = normalizeRoleValue(accountType);
        if (normalized != null) {
            return normalized;
        }

        normalized = normalizeRoleValue(role);
        if (normalized != null) {
            return normalized;
        }

        return "INDIVIDUAL";
    }

    private String normalizeRoleValue(String value) {
        if (isBlank(value)) {
            return null;
        }

        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "SHOPPER", "INDIVIDUAL" -> "INDIVIDUAL";
            case "SELLER", "CORPORATE" -> "CORPORATE";
            case "ADMIN" -> "INDIVIDUAL";
            default -> null;
        };
    }
}
