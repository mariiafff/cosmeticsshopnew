package com.cosmeticsshop.repository;

import com.cosmeticsshop.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByStore_Id(Long storeId);

    Optional<Product> findBySku(String sku);

    Page<Product> findByStore_IdIn(List<Long> storeIds, Pageable pageable);

    Page<Product> findByStatusIgnoreCase(String status, Pageable pageable);

    @Query("""
            select p
            from Product p
            where lower(p.status) = lower(:status)
              and (
                lower(p.name) like lower(concat('%', :search, '%'))
                or lower(coalesce(p.sku, '')) like lower(concat('%', :search, '%'))
                or lower(coalesce(p.stockCode, '')) like lower(concat('%', :search, '%'))
              )
            """)
    Page<Product> searchActiveProducts(String status, String search, Pageable pageable);

    @Query("""
            select p
            from Product p
            where p.store.id in :storeIds
              and (
                :includeInactive = true
                or lower(coalesce(p.status, 'ACTIVE')) = 'active'
              )
            """)
    Page<Product> findByStoreIdsWithStatusScope(
            @Param("storeIds") List<Long> storeIds,
            @Param("includeInactive") boolean includeInactive,
            Pageable pageable
    );

    @Query("""
            select p
            from Product p
            where p.store.id in :storeIds
              and (
                :includeInactive = true
                or lower(coalesce(p.status, 'ACTIVE')) = 'active'
              )
              and (
                lower(p.name) like lower(concat('%', :search, '%'))
                or lower(coalesce(p.sku, '')) like lower(concat('%', :search, '%'))
                or lower(coalesce(p.stockCode, '')) like lower(concat('%', :search, '%'))
              )
            """)
    Page<Product> searchByStoreIds(
            @Param("storeIds") List<Long> storeIds,
            @Param("search") String search,
            @Param("includeInactive") boolean includeInactive,
            Pageable pageable
    );

    Page<Product> findByNameContainingIgnoreCaseOrSkuContainingIgnoreCaseOrStockCodeContainingIgnoreCase(
            String name,
            String sku,
            String stockCode,
            Pageable pageable
    );

    long countByStockQuantityLessThanEqual(Integer stockQuantity);
}
