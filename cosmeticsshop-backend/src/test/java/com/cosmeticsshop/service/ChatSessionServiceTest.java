package com.cosmeticsshop.service;

import com.cosmeticsshop.model.Store;
import com.cosmeticsshop.model.User;
import com.cosmeticsshop.repository.StoreRepository;
import com.cosmeticsshop.repository.UserRepository;
import com.cosmeticsshop.service.ChatSessionService.ChatSession;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatSessionServiceTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolvesSellerTestComWithCorporateRoleAndStore() {
        User seller = new User();
        ReflectionTestUtils.setField(seller, "id", 42L);
        seller.setEmail("seller@test.com");
        seller.setRole("CORPORATE");

        Store store = store(77L, "Luna Marketplace", "OPEN");

        StoreRepositoryCallTracker storeRepositoryTracker = new StoreRepositoryCallTracker();
        UserRepository userRepository = proxy(UserRepository.class, (target, method, args) -> {
            if ("findByEmail".equals(method.getName()) && "seller@test.com".equals(args[0])) {
                return Optional.of(seller);
            }
            return defaultValue(method.getReturnType());
        });
        StoreRepository storeRepository = proxy(StoreRepository.class, (target, method, args) -> {
            if ("findByOwnerUserId".equals(method.getName()) && Long.valueOf(42L).equals(args[0])) {
                storeRepositoryTracker.findByOwnerUserIdCalls++;
                return List.of(store);
            }
            return defaultValue(method.getReturnType());
        });
        HttpServletRequest request = proxy(HttpServletRequest.class, (target, method, args) -> {
            if ("getRemoteAddr".equals(method.getName())) {
                return "127.0.0.1";
            }
            return defaultValue(method.getReturnType());
        });

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "seller@test.com",
                        "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_CORPORATE"))
                )
        );

        ChatSession session = new ChatSessionService(userRepository, storeRepository, request).resolveSession();

        assertEquals("seller@test.com", session.email());
        assertEquals("CORPORATE", session.role());
        assertEquals(42L, session.userId());
        assertEquals(77L, session.storeId());
        assertEquals(1, storeRepositoryTracker.findByOwnerUserIdCalls);
    }

    @Test
    void resolvesSellerTestComToOpenLunaMarketplaceWhenMultipleStoresExist() {
        User seller = new User();
        ReflectionTestUtils.setField(seller, "id", 42L);
        seller.setEmail("seller@test.com");
        seller.setRole("CORPORATE");

        Store olderStore = store(12L, "Luna Beauty", "CLOSED");
        Store currentStore = store(77L, "Luna Marketplace", "OPEN");

        UserRepository userRepository = proxy(UserRepository.class, (target, method, args) -> {
            if ("findByEmail".equals(method.getName()) && "seller@test.com".equals(args[0])) {
                return Optional.of(seller);
            }
            return defaultValue(method.getReturnType());
        });
        StoreRepository storeRepository = proxy(StoreRepository.class, (target, method, args) -> {
            if ("findByOwnerUserId".equals(method.getName()) && Long.valueOf(42L).equals(args[0])) {
                return List.of(olderStore, currentStore);
            }
            return defaultValue(method.getReturnType());
        });
        HttpServletRequest request = proxy(HttpServletRequest.class, (target, method, args) -> {
            if ("getRemoteAddr".equals(method.getName())) {
                return "127.0.0.1";
            }
            return defaultValue(method.getReturnType());
        });

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "seller@test.com",
                        "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_CORPORATE"))
                )
        );

        ChatSession session = new ChatSessionService(userRepository, storeRepository, request).resolveSession();

        assertEquals(77L, session.storeId());
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (target, method, args) -> {
            if ("toString".equals(method.getName())) {
                return type.getSimpleName() + "TestProxy";
            }
            if ("hashCode".equals(method.getName())) {
                return System.identityHashCode(target);
            }
            if ("equals".equals(method.getName())) {
                return target == args[0];
            }
            return handler.invoke(target, method, args == null ? new Object[0] : args);
        });
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType.equals(boolean.class)) {
            return false;
        }
        if (returnType.equals(int.class) || returnType.equals(long.class) || returnType.equals(short.class)
                || returnType.equals(byte.class) || returnType.equals(double.class) || returnType.equals(float.class)) {
            return 0;
        }
        return null;
    }

    private static Store store(Long id, String name, String status) {
        Store store = new Store();
        ReflectionTestUtils.setField(store, "id", id);
        store.setOwnerUserId(42L);
        store.setName(name);
        store.setStatus(status);
        return store;
    }

    private static class StoreRepositoryCallTracker {
        private int findByOwnerUserIdCalls;
    }
}
