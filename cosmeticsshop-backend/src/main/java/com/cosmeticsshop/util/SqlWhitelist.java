package com.cosmeticsshop.util;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SqlWhitelist {

    private static final Map<String, Set<String>> ALLOWED_OBJECTS = buildAllowedObjects();

    private static final Pattern TABLE_REFERENCE_PATTERN =
            Pattern.compile("\\b(?:from|join)\\s+([a-zA-Z0-9_\\.]+)", Pattern.CASE_INSENSITIVE);

    public boolean isAllowed(String objectName) {
        return ALLOWED_OBJECTS.containsKey(normalize(objectName));
    }

    public Set<String> allowedColumnsFor(String objectName) {
        return ALLOWED_OBJECTS.getOrDefault(normalize(objectName), Set.of());
    }

    public Set<String> allowedObjectNames() {
        return ALLOWED_OBJECTS.keySet();
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

    private static Map<String, Set<String>> buildAllowedObjects() {
        Map<String, Set<String>> objects = new LinkedHashMap<>();
        objects.put("ai_safe.products", Set.of(
                "id", "sku", "stock_code", "name", "unit_price", "category_id", "store_id"
        ));
        objects.put("ai_safe.orders", Set.of(
                "id", "invoice_no", "order_date", "status", "grand_total", "normalized_grand_total_usd", "store_id"
        ));
        objects.put("ai_safe.order_items", Set.of(
                "id", "order_id", "product_id", "quantity", "unit_price"
        ));
        objects.put("ai_safe.customer_profiles", Set.of(
                "user_id", "age", "city", "membership_type", "total_spend", "items_purchased",
                "avg_rating", "discount_applied", "satisfaction_level"
        ));
        objects.put("ai_safe.customer_segments", Set.of(
                "user_id", "value_segment", "membership_type", "satisfaction_level"
        ));
        objects.put("ai_safe.city_customer_summary", Set.of(
                "city", "total_customers", "avg_spend"
        ));
        objects.put("ai_safe.country_revenue_summary", Set.of(
                "country", "total_revenue", "total_orders"
        ));
        objects.put("ai_safe.membership_summary", Set.of(
                "membership_type", "total_customers", "avg_spend", "avg_rating"
        ));
        objects.put("ai_safe.segment_summary", Set.of(
                "value_segment", "membership_type", "total_customers"
        ));
        objects.put("ai_safe.reviews", Set.of(
                "id", "product_id", "user_id", "rating", "comment", "title", "seller_response", "created_at"
        ));
        objects.put("ai_safe.stores", Set.of(
                "id", "name", "status", "owner_id"
        ));
        return Map.copyOf(objects);
    }
}
