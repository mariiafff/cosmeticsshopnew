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

    List<Order> findByUserId(Long userId);
}
