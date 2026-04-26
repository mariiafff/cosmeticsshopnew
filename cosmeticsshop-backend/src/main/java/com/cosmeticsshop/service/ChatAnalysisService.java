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

        if (normalized.contains("city") && firstRow.containsKey("city") && firstRow.containsKey("total_customers")) {
            return firstRow.get("city") + " has the highest customer count with "
                    + formatNumber(firstRow.get("total_customers")) + " customers.";
        }

        if (normalized.contains("country") && firstRow.containsKey("country") && firstRow.containsKey("total_revenue")) {
            return firstRow.get("country") + " generates the most revenue: "
                    + formatNumber(firstRow.get("total_revenue")) + ".";
        }

        if (normalized.contains("product") && firstRow.containsKey("product_name")) {
            return "Here are the products that best match your analytics request.";
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

    public record VisualizationPayload(String visualizationType, Map<String, Object> chartData) {
    }
}
