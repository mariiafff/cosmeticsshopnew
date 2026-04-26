package com.cosmeticsshop.controller;

import com.cosmeticsshop.model.Store;
import com.cosmeticsshop.model.User;
import com.cosmeticsshop.repository.UserRepository;
import com.cosmeticsshop.service.StoreService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stores")
public class StoreController {

    private final StoreService storeService;
    private final UserRepository userRepository;

    public StoreController(StoreService storeService, UserRepository userRepository) {
        this.storeService = storeService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<Store> getStores() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return storeService.getAllStores();
        }

        User user = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (user == null) {
            return storeService.getAllStores();
        }

        if ("CORPORATE".equalsIgnoreCase(user.getRole())) {
            return storeService.getStoresByOwnerUserId(user.getId());
        }

        return storeService.getAllStores();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CORPORATE')")
    public Store createStore(@RequestBody Store store) {
        User currentUser = getCurrentUser();
        if ("CORPORATE".equalsIgnoreCase(currentUser.getRole())) {
            return storeService.saveForOwner(currentUser.getId(), store);
        }
        return storeService.save(store);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CORPORATE')")
    public Store updateStore(@PathVariable Long id, @RequestBody Store store) {
        User currentUser = getCurrentUser();
        if ("CORPORATE".equalsIgnoreCase(currentUser.getRole())
                && !storeService.userOwnsStore(currentUser.getId(), id)) {
            throw new IllegalArgumentException("You can only update your own store.");
        }
        return storeService.updateStore(id, store);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CORPORATE')")
    public void deleteStore(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        if ("CORPORATE".equalsIgnoreCase(currentUser.getRole())
                && !storeService.userOwnsStore(currentUser.getId(), id)) {
            throw new IllegalArgumentException("You can only delete your own store.");
        }
        storeService.deleteStore(id);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(authentication.getName()).orElseThrow();
    }
}
