package com.cosmeticsshop.service;

import com.cosmeticsshop.exception.ResourceNotFoundException;
import com.cosmeticsshop.model.Product;
import com.cosmeticsshop.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Page<Product> getProductsPage(int page, int size, String search, String sort) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), normalizeSize(size), buildSort(sort));
        Page<Product> products = (search == null || search.isBlank())
                ? productRepository.findAll(pageable)
                : productRepository.findByNameContainingIgnoreCaseOrSkuContainingIgnoreCaseOrStockCodeContainingIgnoreCase(
                        search.trim(),
                        search.trim(),
                        search.trim(),
                        pageable
                );
        log.info(
                "PRODUCT PAGE FROM REPOSITORY = {} items, total={} page={} size={} search={} sort={}",
                products.getNumberOfElements(),
                products.getTotalElements(),
                products.getNumber(),
                products.getSize(),
                search,
                sort
        );
        return products;
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    public List<Product> getAllProducts(int size) {
        return getProductsPage(0, size, null, "name,asc").getContent();
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
        existingProduct.setStore(product.getStore());
        existingProduct.setSku(product.getSku());
        existingProduct.setStockCode(product.getStockCode());
        existingProduct.setCategory(product.getCategory());
        existingProduct.setStockQuantity(product.getStockQuantity());
        existingProduct.setUnitPrice(product.getUnitPrice());
        existingProduct.setCurrencyCode(product.getCurrencyCode());
        existingProduct.setNormalizedUnitPriceUsd(product.getNormalizedUnitPriceUsd());
        existingProduct.setStyle(product.getStyle());
        existingProduct.setProductImportance(product.getProductImportance());

        return productRepository.save(existingProduct);
    }

    public long countLowStockProducts() {
        return productRepository.countByStockQuantityLessThanEqual(5);
    }

    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return 24;
        }
        return Math.min(size, 5000);
    }

    private Sort buildSort(String sort) {
        String sortValue = (sort == null || sort.isBlank()) ? "name,asc" : sort.trim();
        String[] parts = sortValue.split(",", 2);
        String requestedField = parts[0].trim();
        String field = switch (requestedField.toLowerCase(Locale.ROOT)) {
            case "price", "unitprice", "unit_price" -> "unitPrice";
            case "stock", "stockquantity", "stock_quantity" -> "stockQuantity";
            case "created", "createdat", "created_at" -> "createdAt";
            case "updated", "updatedat", "updated_at" -> "updatedAt";
            case "sku" -> "sku";
            default -> "name";
        };

        Sort.Direction direction = parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Sort.by(direction, field).and(Sort.by(Sort.Direction.ASC, "id"));
    }
}
