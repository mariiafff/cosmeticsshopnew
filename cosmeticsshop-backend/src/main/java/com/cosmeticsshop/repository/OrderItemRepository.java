package com.cosmeticsshop.repository;

import com.cosmeticsshop.dto.TopProductResponse;
import com.cosmeticsshop.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("""
            select oi
            from OrderItem oi
            join fetch oi.order
            join fetch oi.product
            where oi.order.id in :orderIds
            """)
    List<OrderItem> findWithProductByOrderIdIn(List<Long> orderIds);

    @Query("""
            select new com.cosmeticsshop.dto.TopProductResponse(
                p.id,
                p.name,
                sum(oi.quantity),
                sum(oi.quantity * oi.price)
            )
            from OrderItem oi
            join oi.product p
            group by p.id, p.name
            order by sum(oi.quantity) desc
            """)
    List<TopProductResponse> findTopSellingProducts();

    @Query("""
            select new map(
                coalesce(p.category.name, 'Uncategorized') as category,
                sum(oi.quantity * oi.price) as revenue,
                sum(oi.quantity) as units
            )
            from OrderItem oi
            join oi.product p
            left join p.category c
            group by c.name
            order by sum(oi.quantity * oi.price) desc
            """)
    List<java.util.Map<String, Object>> findCategoryRevenue();
}
