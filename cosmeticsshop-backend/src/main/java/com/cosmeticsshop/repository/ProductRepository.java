package com.cosmeticsshop.repository;

import com.cosmeticsshop.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByStore_Id(Long storeId);

    List<Product> findByNameContainingIgnoreCaseOrCategory_NameContainingIgnoreCase(String name, String category);

    long countByStockQuantityLessThanEqual(Integer stockQuantity);
}
