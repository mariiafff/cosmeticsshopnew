package com.cosmeticsshop.service;

import com.cosmeticsshop.dto.ProductResponseDto;
import com.cosmeticsshop.exception.ResourceNotFoundException;
import com.cosmeticsshop.model.Product;
import com.cosmeticsshop.model.Store;
import com.cosmeticsshop.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Page<Product> getProductsPage(int page, int size, String search, String sort) {
        return getProductsPage(page, size, search, sort, false);
    }

    public Page<Product> getProductsPage(int page, int size, String search, String sort, boolean includeInactive) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), normalizeSize(size), buildSort(sort));
        Page<Product> products;
        if (includeInactive) {
            products = (search == null || search.isBlank())
                    ? productRepository.findAll(pageable)
                    : productRepository.findByNameContainingIgnoreCaseOrSkuContainingIgnoreCaseOrStockCodeContainingIgnoreCase(
                            search.trim(),
                            search.trim(),
                            search.trim(),
                            pageable
                    );
        } else {
            products = (search == null || search.isBlank())
                    ? productRepository.findByStatusIgnoreCase("ACTIVE", pageable)
                    : productRepository.searchActiveProducts("ACTIVE", search.trim(), pageable);
        }
        log.info(
                "PRODUCT PAGE FROM REPOSITORY = {} items, total={} page={} size={} search={} sort={} includeInactive={}",
                products.getNumberOfElements(),
                products.getTotalElements(),
                products.getNumber(),
                products.getSize(),
                search,
                sort,
                includeInactive
        );
        return products;
    }

    public Page<ProductResponseDto> getProductResponsePage(int page, int size, String search, String sort, boolean includeInactive) {
        return getProductsPage(page, size, search, sort, includeInactive).map(this::toProductResponseDto);
    }

    public Page<Product> getProductsPageForStores(
            int page,
            int size,
            String search,
            String sort,
            boolean includeInactive,
            List<Long> storeIds
    ) {
        if (storeIds == null || storeIds.isEmpty()) {
            return Page.empty(PageRequest.of(Math.max(page, 0), normalizeSize(size), buildSort(sort)));
        }

        Pageable pageable = PageRequest.of(Math.max(page, 0), normalizeSize(size), buildSort(sort));
        String normalizedSearch = search == null ? null : search.trim();
        Page<Product> products = (normalizedSearch == null || normalizedSearch.isBlank())
                ? productRepository.findByStoreIdsWithStatusScope(
                        storeIds.stream().distinct().toList(),
                        includeInactive,
                        pageable
                )
                : productRepository.searchByStoreIds(
                        storeIds.stream().distinct().toList(),
                        normalizedSearch,
                        includeInactive,
                        pageable
                );
        log.info(
                "STORE PRODUCT PAGE = {} items, total={} stores={} page={} size={} search={} includeInactive={}",
                products.getNumberOfElements(),
                products.getTotalElements(),
                storeIds,
                products.getNumber(),
                products.getSize(),
                search,
                includeInactive
        );
        return products;
    }

    public Page<ProductResponseDto> getProductResponsePageForStores(
            int page,
            int size,
            String search,
            String sort,
            boolean includeInactive,
            List<Long> storeIds
    ) {
        return getProductsPageForStores(page, size, search, sort, includeInactive, storeIds).map(this::toProductResponseDto);
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    public ProductResponseDto getProductResponseById(Long id) {
        return toProductResponseDto(getProductById(id));
    }

    public List<Product> getAllProducts(int size) {
        return getProductsPage(0, size, null, "name,asc").getContent();
    }

    public Product saveProduct(Product product) {
        prepareNewProduct(product);
        try {
            return productRepository.save(product);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Could not create product. Missing required product defaults.");
        }
    }

    public Product saveProductForStore(Product product, Store store) {
        product.setStore(store);
        return saveProduct(product);
    }

    public ProductResponseDto saveProductResponse(Product product) {
        return toProductResponseDto(saveProduct(product));
    }

    public ProductResponseDto saveProductResponseForStore(Product product, Store store) {
        return toProductResponseDto(saveProductForStore(product, store));
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
        existingProduct.setImageUrl(product.getImageUrl());
        existingProduct.setStyle(product.getStyle());
        existingProduct.setProductImportance(product.getProductImportance());
        if (product.getStatus() != null && !product.getStatus().isBlank()) {
            existingProduct.setStatus(product.getStatus());
        }

        prepareExistingProduct(existingProduct);
        try {
            return productRepository.save(existingProduct);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Could not update product. Missing required product defaults.");
        }
    }

    public Product updateProductForStores(Long id, Product product, List<Long> allowedStoreIds, Store forcedStore) {
        Product existingProduct = getProductById(id);
        validateStoreAccess(existingProduct, allowedStoreIds);

        existingProduct.setName(product.getName());
        existingProduct.setPrice(product.getPrice());
        existingProduct.setDescription(product.getDescription());
        existingProduct.setStore(forcedStore != null ? forcedStore : existingProduct.getStore());
        existingProduct.setSku(product.getSku());
        existingProduct.setStockCode(product.getStockCode());
        existingProduct.setCategory(product.getCategory());
        existingProduct.setStockQuantity(product.getStockQuantity());
        existingProduct.setUnitPrice(product.getUnitPrice());
        existingProduct.setCurrencyCode(product.getCurrencyCode());
        existingProduct.setNormalizedUnitPriceUsd(product.getNormalizedUnitPriceUsd());
        existingProduct.setImageUrl(product.getImageUrl());
        existingProduct.setStyle(product.getStyle());
        existingProduct.setProductImportance(product.getProductImportance());
        if (product.getStatus() != null && !product.getStatus().isBlank()) {
            existingProduct.setStatus(product.getStatus());
        }

        prepareExistingProduct(existingProduct);
        try {
            return productRepository.save(existingProduct);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Could not update product. Missing required product defaults.");
        }
    }

    public ProductResponseDto updateProductResponse(Long id, Product product) {
        return toProductResponseDto(updateProduct(id, product));
    }

    public ProductResponseDto updateProductResponseForStores(Long id, Product product, List<Long> allowedStoreIds, Store forcedStore) {
        return toProductResponseDto(updateProductForStores(id, product, allowedStoreIds, forcedStore));
    }

    public long countLowStockProducts() {
        return productRepository.countByStockQuantityLessThanEqual(5);
    }

    public Product deactivateProduct(Long id) {
        Product existingProduct = getProductById(id);
        existingProduct.setStatus("INACTIVE");
        return productRepository.save(existingProduct);
    }

    public Product deactivateProductForStores(Long id, List<Long> allowedStoreIds) {
        Product existingProduct = getProductById(id);
        validateStoreAccess(existingProduct, allowedStoreIds);
        existingProduct.setStatus("INACTIVE");
        return productRepository.save(existingProduct);
    }

    public Product activateProduct(Long id) {
        Product existingProduct = getProductById(id);
        existingProduct.setStatus("ACTIVE");
        return productRepository.save(existingProduct);
    }

    public Product activateProductForStores(Long id, List<Long> allowedStoreIds) {
        Product existingProduct = getProductById(id);
        validateStoreAccess(existingProduct, allowedStoreIds);
        existingProduct.setStatus("ACTIVE");
        return productRepository.save(existingProduct);
    }

    public ProductResponseDto deactivateProductResponse(Long id) {
        return toProductResponseDto(deactivateProduct(id));
    }

    public ProductResponseDto deactivateProductResponseForStores(Long id, List<Long> allowedStoreIds) {
        return toProductResponseDto(deactivateProductForStores(id, allowedStoreIds));
    }

    public ProductResponseDto activateProductResponse(Long id) {
        return toProductResponseDto(activateProduct(id));
    }

    public ProductResponseDto activateProductResponseForStores(Long id, List<Long> allowedStoreIds) {
        return toProductResponseDto(activateProductForStores(id, allowedStoreIds));
    }

    private void validateStoreAccess(Product product, List<Long> allowedStoreIds) {
        if (allowedStoreIds == null || allowedStoreIds.isEmpty()) {
            throw new ResourceNotFoundException("You do not have a store assigned yet.");
        }
        Long productStoreId = product.getStore() == null ? null : product.getStore().getId();
        if (productStoreId == null || !Set.copyOf(allowedStoreIds).contains(productStoreId)) {
            throw new ResourceNotFoundException("You can only manage products from your own store.");
        }
    }

    private void prepareNewProduct(Product product) {
        validateProductInput(product);

        LocalDateTime now = LocalDateTime.now();
        if (product.getSku() == null || product.getSku().isBlank()) {
            product.setSku("MANUAL-" + System.currentTimeMillis());
        }
        if (product.getStockCode() == null || product.getStockCode().isBlank()) {
            product.setStockCode(product.getSku());
        }
        if (product.getStatus() == null || product.getStatus().isBlank()) {
            product.setStatus("ACTIVE");
        }
        if (product.getCurrencyCode() == null || product.getCurrencyCode().isBlank()) {
            product.setCurrencyCode("USD");
        }
        if (product.getStockQuantity() == null) {
            product.setStockQuantity(0);
        }
        if (product.getUnitPrice() == null) {
            product.setUnitPrice(BigDecimal.valueOf(product.getPrice()));
        }
        if (product.getNormalizedUnitPriceUsd() == null) {
            product.setNormalizedUnitPriceUsd(product.getUnitPrice());
        }
        if (product.getProductImportance() == null || product.getProductImportance().isBlank()) {
            product.setProductImportance("MEDIUM");
        }
        if (product.getCreatedAt() == null) {
            product.setCreatedAt(now);
        }
        product.setUpdatedAt(now);
    }

    private void prepareExistingProduct(Product product) {
        validateProductInput(product);

        if (product.getSku() == null || product.getSku().isBlank()) {
            product.setSku("MANUAL-" + System.currentTimeMillis());
        }
        if (product.getStockCode() == null || product.getStockCode().isBlank()) {
            product.setStockCode(product.getSku());
        }
        if (product.getStatus() == null || product.getStatus().isBlank()) {
            product.setStatus("ACTIVE");
        }
        if (product.getCurrencyCode() == null || product.getCurrencyCode().isBlank()) {
            product.setCurrencyCode("USD");
        }
        if (product.getStockQuantity() == null) {
            product.setStockQuantity(0);
        }
        if (product.getUnitPrice() == null) {
            product.setUnitPrice(BigDecimal.valueOf(product.getPrice()));
        }
        if (product.getNormalizedUnitPriceUsd() == null) {
            product.setNormalizedUnitPriceUsd(product.getUnitPrice());
        }
        if (product.getProductImportance() == null || product.getProductImportance().isBlank()) {
            product.setProductImportance("MEDIUM");
        }
        if (product.getCreatedAt() == null) {
            product.setCreatedAt(LocalDateTime.now());
        }
        product.setUpdatedAt(LocalDateTime.now());
    }

    private void validateProductInput(Product product) {
        if (product.getName() == null || product.getName().isBlank()) {
            throw new IllegalArgumentException("Product name is required.");
        }
        if (product.getPrice() < 0) {
            throw new IllegalArgumentException("Product price must be zero or greater.");
        }
    }

    private ProductResponseDto toProductResponseDto(Product product) {
        ProductResponseDto dto = new ProductResponseDto();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setSku(product.getSku());
        dto.setPrice(product.getPrice());
        dto.setStockQuantity(product.getStockQuantity());
        dto.setStatus(product.getStatus());
        dto.setImageUrl(product.getImageUrl());
        dto.setDescription(product.getDescription());

        if (product.getStore() != null) {
            dto.setStoreId(product.getStore().getId());
            dto.setStoreName(product.getStore().getName());
        }

        if (product.getCategory() != null) {
            dto.setCategoryId(product.getCategory().getId());
            dto.setCategoryName(product.getCategory().getName());
        }

        return dto;
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
