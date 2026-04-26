package com.cosmeticsshop.controller;

import com.cosmeticsshop.dto.ProductResponseDto;
import com.cosmeticsshop.model.Product;
import com.cosmeticsshop.model.Store;
import com.cosmeticsshop.model.User;
import com.cosmeticsshop.repository.UserRepository;
import com.cosmeticsshop.service.ProductService;
import com.cosmeticsshop.service.StoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;
    private final StoreService storeService;
    private final UserRepository userRepository;

    public ProductController(ProductService productService, StoreService storeService, UserRepository userRepository) {
        this.productService = productService;
        this.storeService = storeService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public Page<ProductResponseDto> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "name,asc") String sort
    ) {
        String effectiveSearch = (search == null || search.isBlank()) ? q : search;
        Page<ProductResponseDto> products = productService.getProductResponsePage(page, size, effectiveSearch, sort, false);
        log.info(
                "Returning {} products from /api/products page={} size={} total={}",
                products.getNumberOfElements(),
                products.getNumber(),
                products.getSize(),
                products.getTotalElements()
        );
        return products;
    }

    @GetMapping("/manage")
    @PreAuthorize("hasAnyRole('ADMIN', 'CORPORATE')")
    public Page<ProductResponseDto> getManageProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "name,asc") String sort
    ) {
        String effectiveSearch = (search == null || search.isBlank()) ? q : search;
        User currentUser = getCurrentUser();
        if ("CORPORATE".equalsIgnoreCase(currentUser.getRole())) {
            List<Long> storeIds = storeService.getStoresByOwnerUserId(currentUser.getId()).stream()
                    .map(Store::getId)
                    .toList();
            return productService.getProductResponsePageForStores(page, size, effectiveSearch, sort, true, storeIds);
        }
        return productService.getProductResponsePage(page, size, effectiveSearch, sort, true);
    }

    @GetMapping("/{id}")
    public ProductResponseDto getProductById(@PathVariable Long id) {
        return productService.getProductResponseById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CORPORATE')")
    public ProductResponseDto createProduct(@RequestBody Product product) {
        User currentUser = getCurrentUser();
        if ("CORPORATE".equalsIgnoreCase(currentUser.getRole())) {
            Store store = resolveCorporateStore(currentUser, product);
            return productService.saveProductResponseForStore(product, store);
        }
        return productService.saveProductResponse(product);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CORPORATE')")
    public ProductResponseDto updateProduct(@PathVariable Long id, @RequestBody Product product) {
        User currentUser = getCurrentUser();
        if ("CORPORATE".equalsIgnoreCase(currentUser.getRole())) {
            Store store = resolveCorporateStore(currentUser, product);
            List<Long> storeIds = storeService.getStoresByOwnerUserId(currentUser.getId()).stream()
                    .map(Store::getId)
                    .toList();
            return productService.updateProductResponseForStores(id, product, storeIds, store);
        }
        return productService.updateProductResponse(id, product);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CORPORATE')")
    public ProductResponseDto deleteProduct(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        if ("CORPORATE".equalsIgnoreCase(currentUser.getRole())) {
            List<Long> storeIds = storeService.getStoresByOwnerUserId(currentUser.getId()).stream()
                    .map(Store::getId)
                    .toList();
            return productService.deactivateProductResponseForStores(id, storeIds);
        }
        return productService.deactivateProductResponse(id);
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'CORPORATE')")
    public ProductResponseDto activateProduct(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        if ("CORPORATE".equalsIgnoreCase(currentUser.getRole())) {
            List<Long> storeIds = storeService.getStoresByOwnerUserId(currentUser.getId()).stream()
                    .map(Store::getId)
                    .toList();
            return productService.activateProductResponseForStores(id, storeIds);
        }
        return productService.activateProductResponse(id);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(authentication.getName()).orElseThrow();
    }

    private Store resolveCorporateStore(User currentUser, Product product) {
        List<Store> stores = storeService.getStoresByOwnerUserId(currentUser.getId());
        if (stores.isEmpty()) {
            throw new IllegalArgumentException("You do not have a store yet. Create a store to start managing products.");
        }

        if (product.getStore() != null && product.getStore().getId() != null) {
            Long requestedStoreId = product.getStore().getId();
            return stores.stream()
                    .filter(store -> store.getId().equals(requestedStoreId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("You can only assign products to your own store."));
        }

        return stores.get(0);
    }
}
