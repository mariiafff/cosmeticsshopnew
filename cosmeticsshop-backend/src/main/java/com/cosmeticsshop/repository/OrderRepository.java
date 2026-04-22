package com.cosmeticsshop.repository;

import com.cosmeticsshop.model.Order;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("select coalesce(sum(o.totalPrice), 0) from Order o")
    Double findTotalRevenue();

    @Query("select count(o) from Order o")
    Long findTotalOrdersCount();

    List<Order> findByUser_Id(Long userId);

    List<Order> findByStore_Id(Long storeId);

    @Query("""
            select new map(function('formatdatetime', o.createdAt, 'yyyy-MM') as period, coalesce(sum(o.totalPrice), 0) as revenue)
            from Order o
            group by function('formatdatetime', o.createdAt, 'yyyy-MM')
            order by function('formatdatetime', o.createdAt, 'yyyy-MM')
            """)
    List<java.util.Map<String, Object>> findMonthlyRevenue();
}
