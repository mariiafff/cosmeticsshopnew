package com.cosmeticsshop.repository;

import com.cosmeticsshop.model.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoreRepository extends JpaRepository<Store, Long> {

    List<Store> findByOwnerUserId(Long ownerUserId);
}
