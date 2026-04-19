package com.cosmeticsshop.repository;

import com.cosmeticsshop.dto.TopProductResponse;
import com.cosmeticsshop.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

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
}
