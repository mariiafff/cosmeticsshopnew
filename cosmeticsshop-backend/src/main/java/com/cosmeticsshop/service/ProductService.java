package com.cosmeticsshop.service;

import com.cosmeticsshop.exception.ResourceNotFoundException;
import com.cosmeticsshop.model.Product;
import com.cosmeticsshop.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    public List<Product> searchProducts(String query) {
        if (query == null || query.isBlank()) {
            return getAllProducts();
        }
        return productRepository.findByNameContainingIgnoreCaseOrCategory_NameContainingIgnoreCase(query, query);
    }

    public Product saveProduct(Product product) {
        if (product.getSku() == null || product.getSku().isBlank()) {
            product.setSku("SKU-" + System.currentTimeMillis());
        }
        return productRepository.save(product);
    }

    public Product updateProduct(Long id, Product product) {
        Product existingProduct = getProductById(id);

        existingProduct.setName(product.getName());
        existingProduct.setPrice(product.getPrice());
        existingProduct.setDescription(product.getDescription());
        existingProduct.setSellerId(product.getSellerId());
        existingProduct.setStore(product.getStore());
        existingProduct.setSku(product.getSku());
        existingProduct.setCategory(product.getCategory());
        existingProduct.setStockQuantity(product.getStockQuantity());
        existingProduct.setStatus(product.getStatus());
        existingProduct.setAverageRating(product.getAverageRating());

        return productRepository.save(existingProduct);
    }

    public long countLowStockProducts() {
        return productRepository.countByStockQuantityLessThanEqual(5);
    }

    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }
}
