package com.cosmeticsshop.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class QueryExecutionService {

    private final JdbcTemplate jdbcTemplate;

    public QueryExecutionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> executeSelect(String sql) {
        String normalized = sql == null ? "" : sql.trim().toLowerCase(Locale.ROOT);
        if (!normalized.startsWith("select")) {
            throw new IllegalArgumentException("Only SELECT queries are allowed.");
        }
        if (normalized.contains(";")) {
            throw new IllegalArgumentException("Multiple statements are not allowed.");
        }
        return jdbcTemplate.queryForList(sql);
    }
}
