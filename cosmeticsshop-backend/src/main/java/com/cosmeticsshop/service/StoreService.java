package com.cosmeticsshop.service;

import com.cosmeticsshop.exception.ResourceNotFoundException;
import com.cosmeticsshop.model.Store;
import com.cosmeticsshop.repository.StoreRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StoreService {

    private final StoreRepository storeRepository;

    public StoreService(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    public List<Store> getAllStores() {
        return storeRepository.findAll();
    }

    public Store getStoreById(Long id) {
        return storeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found: " + id));
    }

    public Store save(Store store) {
        return storeRepository.save(store);
    }

    public Store updateStore(Long id, Store store) {
        Store existing = getStoreById(id);
        existing.setName(store.getName());
        existing.setCity(store.getCity());
        existing.setCountry(store.getCountry());
        existing.setStatus(store.getStatus());
        existing.setDescription(store.getDescription());
        existing.setOwnerUserId(store.getOwnerUserId());
        return storeRepository.save(existing);
    }

    public void deleteStore(Long id) {
        storeRepository.deleteById(id);
    }
}
