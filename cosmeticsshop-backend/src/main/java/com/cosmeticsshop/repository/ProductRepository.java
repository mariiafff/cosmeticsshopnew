package com.cosmeticsshop.repository;

import com.cosmeticsshop.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByStore_Id(Long storeId);

    Page<Product> findByNameContainingIgnoreCaseOrSkuContainingIgnoreCaseOrStockCodeContainingIgnoreCase(
            String name,
            String sku,
            String stockCode,
            Pageable pageable
    );

    long countByStockQuantityLessThanEqual(Integer stockQuantity);
}
