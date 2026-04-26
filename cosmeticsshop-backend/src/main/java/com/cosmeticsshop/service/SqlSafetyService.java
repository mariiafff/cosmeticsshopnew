package com.cosmeticsshop.service;

import com.cosmeticsshop.util.SqlWhitelist;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SqlSafetyService {

    private static final Pattern SELECT_PATTERN = Pattern.compile("^\\s*select\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern LIMIT_PATTERN = Pattern.compile("\\blimit\\s+(\\d+)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern FROM_OR_JOIN_PATTERN = Pattern.compile(
            "\\b(?:from|join)\\s+(ai_safe\\.[a-z_][a-z0-9_]*)(?:\\s+(?:as\\s+)?([a-z_][a-z0-9_]*))?",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern DOTTED_IDENTIFIER_PATTERN = Pattern.compile("\\b([a-z_][a-z0-9_]*)\\.([a-z_][a-z0-9_]*)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("\\b[a-z_][a-z0-9_]*\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SELECT_ALIAS_PATTERN = Pattern.compile("\\bas\\s+([a-z_][a-z0-9_]*)\\b", Pattern.CASE_INSENSITIVE);

    private static final Set<String> FORBIDDEN_TERMS = Set.of(
            "insert", "update", "delete", "drop", "alter", "create", "truncate",
            "grant", "revoke", "merge", "call", "execute", "union", "information_schema",
            "pg_catalog", "public.", "password_hash", "api_key", "secret", "token",
            "jwt", "internal_cost", "supplier_margin", "cost_price", "is_admin"
    );

    private static final Set<String> SQL_KEYWORDS = Set.of(
            "select", "from", "join", "left", "right", "inner", "outer", "on", "where", "group", "by",
            "order", "limit", "asc", "desc", "as", "and", "or", "not", "is", "null", "in", "like",
            "having", "case", "when", "then", "else", "end", "distinct", "ai_safe"
    );

    private static final Set<String> SQL_FUNCTIONS = Set.of(
            "sum", "count", "avg", "min", "max", "coalesce", "round", "lower", "upper",
            "date_trunc", "extract", "current_date", "current_timestamp", "to_char", "cast"
    );

    public SqlSafetyService(SqlWhitelist sqlWhitelist) {
        this.sqlWhitelist = sqlWhitelist;
    }

    private final SqlWhitelist sqlWhitelist;

    public void validate(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("Generated SQL is empty.");
        }

        String normalizedSql = stripStringLiterals(sql).toLowerCase(Locale.ROOT).trim();
        if (!SELECT_PATTERN.matcher(normalizedSql).find()) {
            throw new IllegalArgumentException("Only SELECT queries are allowed.");
        }

        if (normalizedSql.contains("--") || normalizedSql.contains("/*") || normalizedSql.contains("*/")) {
            throw new IllegalArgumentException("SQL comments are not allowed.");
        }

        if (normalizedSql.contains("select *") || normalizedSql.matches(".*\\b[a-z_][a-z0-9_]*\\.\\*.*")) {
            throw new IllegalArgumentException("SELECT * is not allowed.");
        }

        if (normalizedSql.indexOf(';') >= 0 && !normalizedSql.matches(".*;\\s*$")) {
            throw new IllegalArgumentException("Only one SQL statement is allowed.");
        }
        if (normalizedSql.chars().filter(ch -> ch == ';').count() > 1) {
            throw new IllegalArgumentException("Only one SQL statement is allowed.");
        }

        for (String forbiddenTerm : FORBIDDEN_TERMS) {
            if (normalizedSql.contains(forbiddenTerm)) {
                throw new IllegalArgumentException("Generated SQL contains forbidden content.");
            }
        }

        if (!normalizedSql.contains("ai_safe.")) {
            throw new IllegalArgumentException("SQL must reference only ai_safe views.");
        }

        Matcher limitMatcher = LIMIT_PATTERN.matcher(normalizedSql);
        if (limitMatcher.find()) {
            int limit = Integer.parseInt(limitMatcher.group(1));
            if (limit > 100) {
                throw new IllegalArgumentException("LIMIT cannot exceed 100.");
            }
        }

        Set<String> referencedObjects = sqlWhitelist.extractReferencedObjects(normalizedSql);
        if (referencedObjects.isEmpty()) {
            throw new IllegalArgumentException("SQL must reference at least one whitelisted ai_safe view.");
        }

        Map<String, String> aliasToObject = buildAliasMap(normalizedSql, referencedObjects);
        validateColumns(normalizedSql, referencedObjects, aliasToObject);
    }

    private Map<String, String> buildAliasMap(String sql, Set<String> referencedObjects) {
        Map<String, String> aliases = new HashMap<>();
        for (String object : referencedObjects) {
            if (!sqlWhitelist.isAllowed(object)) {
                throw new IllegalArgumentException("SQL references a non-whitelisted object: " + object);
            }
            aliases.put(simpleName(object), object);
        }

        Matcher matcher = FROM_OR_JOIN_PATTERN.matcher(sql);
        while (matcher.find()) {
            String object = matcher.group(1).toLowerCase(Locale.ROOT);
            String alias = matcher.group(2);
            if (!sqlWhitelist.isAllowed(object)) {
                throw new IllegalArgumentException("SQL references a non-whitelisted object: " + object);
            }
            if (alias != null && !SQL_KEYWORDS.contains(alias.toLowerCase(Locale.ROOT))) {
                aliases.put(alias.toLowerCase(Locale.ROOT), object);
            }
        }

        return aliases;
    }

    private void validateColumns(String sql, Set<String> referencedObjects, Map<String, String> aliasToObject) {
        Set<String> allowedColumns = new HashSet<>();
        for (String object : referencedObjects) {
            allowedColumns.addAll(sqlWhitelist.allowedColumnsFor(object));
        }

        Set<String> selectAliases = new LinkedHashSet<>();
        Matcher aliasMatcher = SELECT_ALIAS_PATTERN.matcher(sql);
        while (aliasMatcher.find()) {
            selectAliases.add(aliasMatcher.group(1).toLowerCase(Locale.ROOT));
        }

        Matcher dottedMatcher = DOTTED_IDENTIFIER_PATTERN.matcher(sql);
        while (dottedMatcher.find()) {
            String qualifier = dottedMatcher.group(1).toLowerCase(Locale.ROOT);
            String column = dottedMatcher.group(2).toLowerCase(Locale.ROOT);
            if ("ai_safe".equals(qualifier)) {
                continue;
            }
            String object = aliasToObject.get(qualifier);
            if (object == null) {
                throw new IllegalArgumentException("SQL references an unknown alias or object: " + qualifier);
            }
            if (!sqlWhitelist.allowedColumnsFor(object).contains(column)) {
                throw new IllegalArgumentException("SQL references a non-whitelisted column: " + column);
            }
        }

        String withoutDottedReferences = DOTTED_IDENTIFIER_PATTERN.matcher(sql).replaceAll(" ");
        Matcher identifierMatcher = IDENTIFIER_PATTERN.matcher(withoutDottedReferences);
        while (identifierMatcher.find()) {
            String identifier = identifierMatcher.group().toLowerCase(Locale.ROOT);
            if (SQL_KEYWORDS.contains(identifier) || SQL_FUNCTIONS.contains(identifier) || selectAliases.contains(identifier)) {
                continue;
            }
            if (aliasToObject.containsKey(identifier)) {
                continue;
            }
            if (!allowedColumns.contains(identifier)) {
                throw new IllegalArgumentException("SQL references an unavailable column: " + identifier);
            }
        }
    }

    private String stripStringLiterals(String sql) {
        return sql.replaceAll("'([^']|'')*'", "''");
    }

    private String simpleName(String objectName) {
        int separatorIndex = objectName.lastIndexOf('.');
        return separatorIndex >= 0 ? objectName.substring(separatorIndex + 1) : objectName;
    }
}
