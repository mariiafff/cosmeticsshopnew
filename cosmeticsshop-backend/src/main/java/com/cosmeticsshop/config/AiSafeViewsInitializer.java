package com.cosmeticsshop.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Configuration
public class AiSafeViewsInitializer {

    private static final Logger log = LoggerFactory.getLogger(AiSafeViewsInitializer.class);

    @Bean
    @org.springframework.core.annotation.Order(100)
    public CommandLineRunner ensureAiSafeSellerViews(JdbcTemplate jdbcTemplate) {
        return args -> {
            jdbcTemplate.execute("create schema if not exists ai_safe");
            createSellerProductSalesSummary(jdbcTemplate);
            createSellerRecentSoldProducts(jdbcTemplate);
            createSellerCustomerSummary(jdbcTemplate);
            createSellerRevenueSummary(jdbcTemplate);
            createUserRecentOrders(jdbcTemplate);
            createUserOrderItems(jdbcTemplate);
            createUserOrderSummary(jdbcTemplate);
            log.info("Ensured ai_safe seller-scoped and user-scoped analytics views exist.");
            logSellerVerification(jdbcTemplate);
        };
    }

    private void createSellerProductSalesSummary(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("""
                create or replace view ai_safe.seller_product_sales_summary as
                select
                    s.id as store_id,
                    s.owner_id as seller_user_id,
                    p.id as product_id,
                    p.name as product_name,
                    coalesce(sum(oi.quantity), 0) as total_quantity,
                    coalesce(sum(oi.quantity * oi.unit_price), 0) as total_revenue,
                    max(o.order_date) as last_order_date
                from public.stores s
                join public.products p on p.store_id = s.id
                left join public.order_items oi on oi.product_id = p.id
                left join public.orders o on o.id = oi.order_id
                group by s.id, s.owner_id, p.id, p.name
                """);
    }

    private void createSellerRecentSoldProducts(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("""
                create or replace view ai_safe.seller_recent_sold_products as
                select
                    s.id as store_id,
                    s.owner_id as seller_user_id,
                    p.id as product_id,
                    p.name as product_name,
                    o.id as order_id,
                    o.order_date,
                    oi.quantity,
                    oi.unit_price
                from public.orders o
                join public.order_items oi on oi.order_id = o.id
                join public.products p on p.id = oi.product_id
                join public.stores s on s.id = p.store_id
                where lower(coalesce(o.status, '')) in ('delivered', 'completed', 'paid', 'placed')
                """);
    }

    private void createSellerCustomerSummary(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("""
                create or replace view ai_safe.seller_customer_summary as
                select
                    s.id as store_id,
                    s.owner_id as seller_user_id,
                    count(distinct o.user_id) as total_customers,
                    count(distinct o.id) as total_orders
                from public.stores s
                left join public.products p on p.store_id = s.id
                left join public.order_items oi on oi.product_id = p.id
                left join public.orders o on o.id = oi.order_id
                group by s.id, s.owner_id
                """);
    }

    private void createSellerRevenueSummary(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("""
                create or replace view ai_safe.seller_revenue_summary as
                select
                    s.id as store_id,
                    s.owner_id as seller_user_id,
                    coalesce(sum(oi.quantity * oi.unit_price), 0) as total_revenue,
                    count(distinct o.id) as total_orders,
                    coalesce(sum(oi.quantity), 0) as total_items_sold
                from public.stores s
                left join public.products p on p.store_id = s.id
                left join public.order_items oi on oi.product_id = p.id
                left join public.orders o on o.id = oi.order_id
                group by s.id, s.owner_id
                """);
    }

    private void createUserRecentOrders(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("""
                create or replace view ai_safe.user_recent_orders as
                select
                    o.id as order_id,
                    o.user_id as customer_id,
                    coalesce(o.order_date, o.created_at) as order_date,
                    coalesce(o.normalized_grand_total_usd, o.grand_total, 0) as total_amount
                from public.orders o
                where lower(coalesce(o.status, '')) in ('delivered', 'completed', 'paid', 'placed')
                """);
    }

    private void createUserOrderItems(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("""
                create or replace view ai_safe.user_order_items as
                select
                    oi.order_id,
                    p.name as product_name,
                    oi.quantity,
                    oi.unit_price
                from public.order_items oi
                join public.products p on p.id = oi.product_id
                """);
    }

    private void createUserOrderSummary(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("""
                create or replace view ai_safe.user_order_summary as
                select
                    o.user_id as customer_id,
                    count(distinct o.id) as total_orders,
                    coalesce(avg(coalesce(o.normalized_grand_total_usd, o.grand_total, 0)), 0) as avg_order_value,
                    coalesce(sum(coalesce(o.normalized_grand_total_usd, o.grand_total, 0)), 0) as total_spent
                from public.orders o
                where lower(coalesce(o.status, '')) in ('delivered', 'completed', 'paid', 'placed')
                group by o.user_id
                """);
    }

    private void logSellerVerification(JdbcTemplate jdbcTemplate) {
        List<Map<String, Object>> users = jdbcTemplate.queryForList("""
                select id, email, role_type
                from public.users
                where email = 'seller@test.com'
                """);
        if (users.isEmpty()) {
            log.warn("seller_ai_safe verification user seller@test.com not found");
            return;
        }

        Number sellerIdNumber = (Number) users.get(0).get("id");
        Long sellerId = sellerIdNumber == null ? null : sellerIdNumber.longValue();
        List<Map<String, Object>> stores = jdbcTemplate.queryForList("""
                select id, name, owner_id
                from public.stores
                where owner_id = ?
                order by case when lower(status) = 'open' then 0 else 1 end, id
                """, sellerId);
        if (stores.isEmpty()) {
            log.warn("seller_ai_safe verification sellerId={} has no store", sellerId);
            return;
        }

        Number storeIdNumber = (Number) stores.get(0).get("id");
        Long storeId = storeIdNumber == null ? null : storeIdNumber.longValue();
        Long productCount = jdbcTemplate.queryForObject(
                "select count(*) from public.products where store_id = ?",
                Long.class,
                storeId
        );
        Long orderItemCount = jdbcTemplate.queryForObject("""
                select count(*)
                from public.order_items oi
                join public.products p on p.id = oi.product_id
                where p.store_id = ?
                """, Long.class, storeId);
        Long orderCount = jdbcTemplate.queryForObject("""
                select count(distinct o.id)
                from public.orders o
                join public.order_items oi on oi.order_id = o.id
                join public.products p on p.id = oi.product_id
                where p.store_id = ?
                """, Long.class, storeId);
        LocalDateTime latestOrderDate = jdbcTemplate.queryForObject("""
                select max(coalesce(o.order_date, o.created_at))
                from public.orders o
                join public.order_items oi on oi.order_id = o.id
                join public.products p on p.id = oi.product_id
                where p.store_id = ?
                """, LocalDateTime.class, storeId);
        List<Map<String, Object>> recentSoldRows = jdbcTemplate.queryForList("""
                select product_name, order_date, quantity, unit_price
                from ai_safe.seller_recent_sold_products
                where store_id = ?
                order by order_date desc
                limit 5
                """, storeId);
        List<Map<String, Object>> productSalesRows = jdbcTemplate.queryForList("""
                select product_name, total_quantity, total_revenue
                from ai_safe.seller_product_sales_summary
                where store_id = ? and total_quantity > 0
                order by total_quantity desc
                limit 5
                """, storeId);

        log.info(
                "seller_ai_safe verification userRows={} storeRows={} productCount={} orderItemCount={} orderCount={} latestOrderDate={} recentSoldRows={} productSalesRows={}",
                users,
                stores,
                productCount,
                orderItemCount,
                orderCount,
                latestOrderDate,
                recentSoldRows,
                productSalesRows
        );
    }
}
