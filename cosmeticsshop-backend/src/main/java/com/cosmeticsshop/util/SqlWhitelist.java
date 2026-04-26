package com.cosmeticsshop.util;

import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SqlWhitelist {

    private static final Set<String> ALLOWED_OBJECTS = Set.of(
            "ai_safe.products",
            "ai_safe.orders",
            "ai_safe.order_items",
            "ai_safe.customer_profiles",
            "ai_safe.customer_segments",
            "ai_safe.product_sales_summary",
            "ai_safe.city_customer_summary",
            "ai_safe.country_revenue_summary",
            "ai_safe.membership_summary",
            "ai_safe.segment_summary"
    );

    private static final Pattern TABLE_REFERENCE_PATTERN =
            Pattern.compile("\\b(?:from|join)\\s+([a-zA-Z0-9_\\.]+)", Pattern.CASE_INSENSITIVE);

    public boolean isAllowed(String objectName) {
        return ALLOWED_OBJECTS.contains(normalize(objectName));
    }

    public Set<String> extractReferencedObjects(String sql) {
        Set<String> referencedObjects = new LinkedHashSet<>();
        Matcher matcher = TABLE_REFERENCE_PATTERN.matcher(sql);
        while (matcher.find()) {
            referencedObjects.add(normalize(matcher.group(1)));
        }
        return referencedObjects;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
