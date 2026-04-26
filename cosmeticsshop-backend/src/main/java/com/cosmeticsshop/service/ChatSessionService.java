package com.cosmeticsshop.service;

import com.cosmeticsshop.model.Store;
import com.cosmeticsshop.model.User;
import com.cosmeticsshop.repository.StoreRepository;
import com.cosmeticsshop.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatSessionService {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final HttpServletRequest request;

    public ChatSessionService(UserRepository userRepository, StoreRepository storeRepository, HttpServletRequest request) {
        this.userRepository = userRepository;
        this.storeRepository = storeRepository;
        this.request = request;
    }

    public ChatSession resolveSession() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return new ChatSession(false, "ANONYMOUS", null, null, null, clientIdentifier());
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return new ChatSession(false, "ANONYMOUS", null, null, null, clientIdentifier());
        }

        Long storeId = null;
        if ("CORPORATE".equalsIgnoreCase(user.getRole())) {
            List<Store> stores = storeRepository.findByOwnerUserId(user.getId());
            if (!stores.isEmpty()) {
                storeId = stores.get(0).getId();
            }
        }

        return new ChatSession(true, normalizeRole(user.getRole()), email, user.getId(), storeId, clientIdentifier());
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "INDIVIDUAL";
        }
        return role.trim().toUpperCase();
    }

    private String clientIdentifier() {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public record ChatSession(
            boolean authenticated,
            String role,
            String email,
            Long userId,
            Long storeId,
            String clientKey
    ) {
    }
}
