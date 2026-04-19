package com.cosmeticsshop.repository;

import com.cosmeticsshop.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}