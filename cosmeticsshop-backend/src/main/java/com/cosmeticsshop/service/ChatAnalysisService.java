package com.cosmeticsshop.service;

import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ChatAnalysisService {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.##");

    public String buildFinalAnswer(String question, List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return "No rows were returned for this question.";
        }

        Map<String, Object> firstRow = rows.get(0);
        String normalized = question == null ? "" : question.toLowerCase(Locale.ROOT);

        if (normalized.contains("membership") && firstRow.containsKey("membership_type") && firstRow.containsKey("avg_spend")) {
            return firstRow.get("membership_type") + " is the membership type with the highest average spend: "
                    + formatNumber(firstRow.get("avg_spend")) + ".";
        }

        if (normalized.contains("city") && firstRow.containsKey("city") && (firstRow.containsKey("total_customers") || firstRow.containsKey("customer_count"))) {
            Object customerCount = firstRow.containsKey("customer_count")
                    ? firstRow.get("customer_count")
                    : firstRow.get("total_customers");
            return firstRow.get("city") + " has the highest customer count with "
                    + formatNumber(customerCount) + " customers.";
        }

        if (normalized.contains("country") && firstRow.containsKey("country") && firstRow.containsKey("total_revenue")) {
            return firstRow.get("country") + " generates the most revenue: "
                    + formatNumber(firstRow.get("total_revenue")) + ".";
        }

        if (isUserScopedQuestion(normalized) && firstRow.containsKey("percentage_of_total_spent")) {
            return "Your last order was "
                    + formatNumber(firstRow.get("percentage_of_total_spent"))
                    + "% of your total spending.";
        }

        if (isUserScopedQuestion(normalized) && firstRow.containsKey("product_name")) {
            return buildLatestOrderItemsAnswer(firstRow, rows);
        }

        if (isUserScopedQuestion(normalized) && firstRow.containsKey("total_spent")) {
            return "Your total spending is "
                    + formatNumber(firstRow.get("total_spent"))
                    + ".";
        }

        if (isUserScopedQuestion(normalized) && firstRow.containsKey("total_amount")) {
            return "Your latest order total is "
                    + formatNumber(firstRow.get("total_amount"))
                    + ".";
        }

        if (normalized.contains("product") && firstRow.containsKey("product_name")) {
            return "Here are the products that best match your analytics request.";
        }

        if ((normalized.contains("mağaza") || normalized.contains("magaza") || normalized.contains("store"))
                && (normalized.contains("ürün") || normalized.contains("urun") || normalized.contains("product"))
                && firstRow.containsKey("product_name")) {
            return "Mağazanızdan alışveriş yapılan en son ürün: "
                    + firstRow.get("product_name")
                    + ".";
        }

        if ((normalized.contains("gelir") || normalized.contains("revenue")) && firstRow.containsKey("total_revenue")) {
            return "Mağazanızın toplam geliri: "
                    + formatNumber(firstRow.get("total_revenue"))
                    + ".";
        }

        return "I found " + rows.size() + " rows based on your question.";
    }

    public VisualizationPayload buildVisualization(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return new VisualizationPayload("NONE", Map.of());
        }

        Map<String, Object> firstRow = rows.get(0);
        if (firstRow.size() < 2) {
            return new VisualizationPayload("TABLE", Map.of());
        }

        String labelKey = null;
        String valueKey = null;
        for (Map.Entry<String, Object> entry : firstRow.entrySet()) {
            if (labelKey == null && entry.getValue() instanceof String) {
                labelKey = entry.getKey();
            } else if (valueKey == null && entry.getValue() instanceof Number) {
                valueKey = entry.getKey();
            }
        }

        if (labelKey == null || valueKey == null) {
            return new VisualizationPayload("TABLE", Map.of());
        }

        List<String> labels = new ArrayList<>();
        List<Number> values = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Object label = row.get(labelKey);
            Object value = row.get(valueKey);
            if (label != null && value instanceof Number number) {
                labels.add(String.valueOf(label));
                values.add(number);
            }
        }

        if (labels.isEmpty()) {
            return new VisualizationPayload("TABLE", Map.of());
        }

        Map<String, Object> chartData = new LinkedHashMap<>();
        chartData.put("labels", labels);
        chartData.put("values", values);
        chartData.put("type", "bar");
        return new VisualizationPayload("BAR", chartData);
    }

    public List<Map<String, Object>> sanitizeRows(List<Map<String, Object>> rows) {
        List<Map<String, Object>> sanitized = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> sanitizedRow = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof String stringValue) {
                    sanitizedRow.put(entry.getKey(), escapeHtml(stringValue));
                } else {
                    sanitizedRow.put(entry.getKey(), value);
                }
            }
            sanitized.add(sanitizedRow);
        }
        return sanitized;
    }

    public String escapeHtml(String value) {
        if (value == null) {
            return null;
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String formatNumber(Object value) {
        if (value instanceof Number number) {
            return DECIMAL_FORMAT.format(number.doubleValue());
        }
        return String.valueOf(value);
    }

    private String buildLatestOrderItemsAnswer(Map<String, Object> firstRow, List<Map<String, Object>> rows) {
        StringBuilder answer = new StringBuilder();
        if (firstRow.containsKey("total_amount")) {
            answer.append("Your latest order total is ")
                    .append(formatNumber(firstRow.get("total_amount")))
                    .append(". It includes:");
        } else {
            answer.append("Your latest order includes:");
        }

        for (Map<String, Object> row : rows) {
            answer.append("\n- ")
                    .append(row.get("product_name"));
            if (row.containsKey("quantity")) {
                answer.append(" x").append(formatNumber(row.get("quantity")));
            }
            if (row.containsKey("line_total")) {
                answer.append(" = ").append(formatNumber(row.get("line_total")));
            } else if (row.containsKey("unit_price")) {
                answer.append(" @ ").append(formatNumber(row.get("unit_price")));
            }
        }

        return answer.toString();
    }

    private boolean isUserScopedQuestion(String normalized) {
        if (normalized.contains("mağaza") || normalized.contains("magaza") || normalized.contains("store")) {
            return false;
        }
        return normalized.contains("benim")
                || normalized.contains("aldığım")
                || normalized.contains("aldigim")
                || normalized.contains("alışveriş")
                || normalized.contains("alisveris")
                || normalized.contains("sipariş")
                || normalized.contains("siparis")
                || normalized.contains("my order")
                || normalized.contains("my purchase")
                || normalized.contains("my spending");
    }

    public record VisualizationPayload(String visualizationType, Map<String, Object> chartData) {
    }
}
