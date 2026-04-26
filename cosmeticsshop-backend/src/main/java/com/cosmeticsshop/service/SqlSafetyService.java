package com.cosmeticsshop.service;

import com.cosmeticsshop.util.SqlWhitelist;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class SqlSafetyService {

    private static final Pattern SELECT_PATTERN = Pattern.compile("^\\s*select\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern FORBIDDEN_KEYWORDS = Pattern.compile(
            "\\b(insert|update|delete|drop|alter|truncate|create|grant|revoke)\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final List<String> FORBIDDEN_REFERENCES = List.of(
            "users",
            "raw_",
            "public.",
            "information_schema",
            "pg_catalog"
    );

    private final SqlWhitelist sqlWhitelist;

    public SqlSafetyService(SqlWhitelist sqlWhitelist) {
        this.sqlWhitelist = sqlWhitelist;
    }

    public void validate(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("Generated SQL is empty.");
        }
        if (!SELECT_PATTERN.matcher(sql).find()) {
            throw new IllegalArgumentException("Only SELECT queries are allowed.");
        }
        if (sql.contains(";")) {
            throw new IllegalArgumentException("Semicolons are not allowed.");
        }
        if (FORBIDDEN_KEYWORDS.matcher(sql).find()) {
            throw new IllegalArgumentException("Generated SQL contains forbidden keywords.");
        }

        String normalizedSql = sql.toLowerCase(Locale.ROOT);
        if (!normalizedSql.contains("ai_safe.")) {
            throw new IllegalArgumentException("SQL must reference ai_safe objects.");
        }

        for (String forbiddenReference : FORBIDDEN_REFERENCES) {
            if (normalizedSql.contains(forbiddenReference)) {
                throw new IllegalArgumentException("SQL contains forbidden references.");
            }
        }

        Set<String> referencedObjects = sqlWhitelist.extractReferencedObjects(sql);
        if (referencedObjects.isEmpty()) {
            throw new IllegalArgumentException("SQL must reference at least one whitelisted ai_safe object.");
        }

        for (String referencedObject : referencedObjects) {
            if (!sqlWhitelist.isAllowed(referencedObject)) {
                throw new IllegalArgumentException("SQL references a non-whitelisted object: " + referencedObject);
            }
        }
    }
}
