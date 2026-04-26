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
}
