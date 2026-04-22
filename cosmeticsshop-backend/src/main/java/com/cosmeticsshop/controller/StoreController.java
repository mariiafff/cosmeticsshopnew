package com.cosmeticsshop.controller;

import com.cosmeticsshop.model.Store;
import com.cosmeticsshop.service.StoreService;
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

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @GetMapping
    public List<Store> getStores() {
        return storeService.getAllStores();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CORPORATE')")
    public Store createStore(@RequestBody Store store) {
        return storeService.save(store);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CORPORATE')")
    public Store updateStore(@PathVariable Long id, @RequestBody Store store) {
        return storeService.updateStore(id, store);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CORPORATE')")
    public void deleteStore(@PathVariable Long id) {
        storeService.deleteStore(id);
    }
}
