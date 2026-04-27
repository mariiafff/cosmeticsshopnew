package com.cosmeticsshop.controller;

import com.cosmeticsshop.repository.ProductRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/debug/db")
public class DebugDbController {

    private final JdbcTemplate jdbcTemplate;
    private final ProductRepository productRepository;

    public DebugDbController(JdbcTemplate jdbcTemplate, ProductRepository productRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.productRepository = productRepository;
    }

    @GetMapping("/users")
    public Map<String, Object> getUsersDebugInfo() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("users", jdbcTemplate.queryForList("select email, role from public.users order by role"));
        return response;
    }

    @GetMapping("/products-count")
    public Map<String, Object> getProductsDebugInfo() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("database", jdbcTemplate.queryForObject("select current_database()", String.class));
        response.put("schema", jdbcTemplate.queryForObject("select current_schema()", String.class));
        response.put("user", jdbcTemplate.queryForObject("select current_user", String.class));
        response.put("productsCountJdbc", jdbcTemplate.queryForObject("select count(*) from public.products", Long.class));
        response.put("productsCountJpa", productRepository.count());
        response.put(
                "sampleRows",
                jdbcTemplate.queryForList(
                        "select id, sku, stock_code, name, unit_price from public.products order by id limit 5"
                )
        );
        return response;
    }

    @GetMapping("/seller-sales-summary")
    public Map<String, Object> getSellerSalesSummary() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("currentMonth", jdbcTemplate.queryForObject("select to_char(current_date, 'YYYY-MM')", String.class));
        response.put(
                "sellers",
                jdbcTemplate.queryForList(
                        """
                        select
                            u.id as user_id,
                            u.email,
                            u.role_type as role,
                            s.id as store_id,
                            s.name as store_name,
                            count(distinct p.id) as products_in_store,
                            count(distinct o.id) as orders_containing_store_products,
                            coalesce(sum(
                                case
                                    when o.created_at >= date_trunc('month', current_date)
                                     and o.created_at < date_trunc('month', current_date) + interval '1 month'
                                    then oi.quantity
                                    else 0
                                end
                            ), 0) as total_quantity_sold_this_month,
                            coalesce(sum(
                                case
                                    when o.created_at >= date_trunc('month', current_date)
                                     and o.created_at < date_trunc('month', current_date) + interval '1 month'
                                    then oi.quantity * oi.unit_price
                                    else 0
                                end
                            ), 0) as total_revenue_this_month
                        from public.users u
                        left join public.stores s on s.owner_id = u.id
                        left join public.products p on p.store_id = s.id
                        left join public.order_items oi on oi.product_id = p.id
                        left join public.orders o on o.id = oi.order_id
                        where upper(u.role_type) = 'CORPORATE'
                        group by u.id, u.email, u.role_type, s.id, s.name
                        order by total_quantity_sold_this_month desc, products_in_store desc, u.email
                        """
                )
        );
        return response;
    }
}
