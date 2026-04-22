package com.cosmeticsshop.service;

import com.cosmeticsshop.dto.ChatResponse;
import com.cosmeticsshop.model.User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ChatbotService {

    private static final String OUT_OF_SCOPE = "This assistant only answers e-commerce analytics questions about products, orders, customers, reviews, stores, revenue, and shipments.";

    private final QueryExecutionService queryExecutionService;

    public ChatbotService(QueryExecutionService queryExecutionService) {
        this.queryExecutionService = queryExecutionService;
    }

    public ChatResponse ask(String question, User user) {
        String normalized = question == null ? "" : question.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Question is required.");
        }

        if (isGreeting(normalized)) {
            return new ChatResponse(question, true, "", "Hello! Ask me about sales, products, customers, reviews, or shipments.", "none", List.of());
        }

        if (!isInScope(normalized)) {
            return new ChatResponse(question, false, "", OUT_OF_SCOPE, "none", List.of());
        }

        String sql = buildSql(normalized, user);
        List<Map<String, Object>> rows = queryExecutionService.executeSelect(sql);
        String answer = rows.isEmpty()
                ? "No matching records were found."
                : "I found " + rows.size() + " result row(s) for your question.";
        String visualizationHint = normalized.contains("trend") || normalized.contains("monthly")
                ? "line"
                : normalized.contains("category") || normalized.contains("share") ? "bar" : "table";

        return new ChatResponse(question, true, sql, answer, visualizationHint, rows);
    }

    private boolean isGreeting(String normalized) {
        return normalized.equals("hi") || normalized.equals("hello") || normalized.equals("hey");
    }

    private boolean isInScope(String normalized) {
        return List.of("sales", "revenue", "order", "product", "customer", "review", "shipment", "store", "category", "analytics")
                .stream()
                .anyMatch(normalized::contains);
    }

    private String buildSql(String normalized, User user) {
        String role = user.getRole();
        if (normalized.contains("top") && normalized.contains("product")) {
            return """
                    select p.name as product_name, sum(oi.quantity) as units_sold, round(sum(oi.quantity * oi.price), 2) as revenue
                    from order_items oi
                    join products p on p.id = oi.product_id
                    group by p.name
                    order by units_sold desc
                    limit 5
                    """;
        }
        if (normalized.contains("category")) {
            return """
                    select coalesce(c.name, 'Uncategorized') as category, round(sum(o.total_price), 2) as revenue, count(distinct o.id) as orders
                    from orders o
                    join order_items oi on oi.order_id = o.id
                    join products p on p.id = oi.product_id
                    left join categories c on c.id = p.category_id
                    group by c.name
                    order by revenue desc
                    """;
        }
        if (normalized.contains("shipment")) {
            return role.equals("INDIVIDUAL")
                    ? "select tracking_number, status, mode_of_shipment, estimated_delivery_at from shipments s join orders o on o.id = s.order_id where o.user_id = " + user.getId() + " order by s.id desc"
                    : "select tracking_number, status, mode_of_shipment, estimated_delivery_at from shipments order by id desc";
        }
        if (normalized.contains("my") || role.equals("INDIVIDUAL")) {
            return "select order_number, status, shipment_status, total_price, created_at from orders where user_id = " + user.getId() + " order by created_at desc";
        }
        return """
                select order_number, status, total_price, created_at
                from orders
                order by created_at desc
                limit 10
                """;
    }
}
