package com.cosmeticsshop.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class QueryExecutionService {

    private final JdbcTemplate jdbcTemplate;

    public QueryExecutionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> executeQuery(String sql) {
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> executeQuery(String sql, Object... args) {
        return jdbcTemplate.queryForList(sql, args);
    }
}
