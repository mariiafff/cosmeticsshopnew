package com.cosmeticsshop.service;

import com.cosmeticsshop.dto.ChatRequest;
import com.cosmeticsshop.dto.ChatResponse;
import com.cosmeticsshop.service.ChatSessionService.ChatSession;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatServiceSellerIntentTest {

    @Test
    void topSellingProductsUsesAllTimeSellerSafeViewAndReturnsRows() {
        FakeQueryExecutionService queryExecutionService = new FakeQueryExecutionService(List.of(
                Map.of("product_name", "Luna Vitamin C Serum", "total_sold", 12),
                Map.of("product_name", "Luna Barrier Repair Cream", "total_sold", 9),
                Map.of("product_name", "Luna Gentle Gel Cleanser", "total_sold", 7)
        ));
        ChatAnalysisService chatAnalysisService = new ChatAnalysisService();
        ChatService chatService = new ChatService(
                queryExecutionService,
                new GuardrailsService(),
                new FixedChatSessionService(new ChatSession(true, "CORPORATE", "seller@test.com", 42L, 77L, "127.0.0.1")),
                new ChatRateLimitService(),
                chatAnalysisService,
                null,
                new DisabledPythonAiChatClient(chatAnalysisService),
                null,
                null,
                null
        );
        ChatRequest request = new ChatRequest();
        request.setQuestion("Top selling products");

        ChatResponse response = chatService.ask(request);

        assertEquals("SUCCESS", response.getStatus());
        assertTrue(response.getGeneratedSql().contains("ai_safe.seller_product_sales_summary"));
        assertTrue(response.getGeneratedSql().contains("store_id = 77"));
        assertTrue(response.getGeneratedSql().contains("limit 5"));
        assertEquals(List.of(77L, 5), queryExecutionService.lastArgs);
        assertEquals(3, response.getRows().size());
        assertTrue(response.getFinalAnswer().contains("Luna Vitamin C Serum"));
    }

    @Test
    void latestOrderedProductsReturnsProductRowsNotOrderRows() {
        FakeQueryExecutionService queryExecutionService = new FakeQueryExecutionService(List.of(
                Map.of("product_name", "Luna Vitamin C Serum", "order_id", 16420, "order_date", "2026-05-12", "quantity", 1, "unit_price", 34.9, "line_total", 34.9),
                Map.of("product_name", "Luna Barrier Repair Cream", "order_id", 16420, "order_date", "2026-05-12", "quantity", 2, "unit_price", 42.5, "line_total", 85.0)
        ));
        ChatAnalysisService chatAnalysisService = new ChatAnalysisService();
        ChatService chatService = new ChatService(
                queryExecutionService,
                new GuardrailsService(),
                new FixedChatSessionService(new ChatSession(true, "CORPORATE", "seller@test.com", 42L, 77L, "127.0.0.1")),
                new ChatRateLimitService(),
                chatAnalysisService,
                null,
                new DisabledPythonAiChatClient(chatAnalysisService),
                null,
                null,
                null
        );
        ChatRequest request = new ChatRequest();
        request.setQuestion("mağazamdaki en son sipariş edilen ürünleri göster");

        ChatResponse response = chatService.ask(request);

        assertEquals("SUCCESS", response.getStatus());
        assertTrue(response.getGeneratedSql().contains("ai_safe.seller_recent_sold_products"));
        assertTrue(response.getGeneratedSql().contains("product_name"));
        assertEquals(List.of(77L, 10), queryExecutionService.lastArgs);
        assertEquals(2, response.getRows().size());
        assertTrue(response.getFinalAnswer().contains("sipariş #16420"));
    }

    private static class FixedChatSessionService extends ChatSessionService {
        private final ChatSession session;

        FixedChatSessionService(ChatSession session) {
            super(null, null, null);
            this.session = session;
        }

        @Override
        public ChatSession resolveSession() {
            return session;
        }
    }

    private static class FakeQueryExecutionService extends QueryExecutionService {
        private final List<Map<String, Object>> rows;
        private List<Object> lastArgs = List.of();

        FakeQueryExecutionService(List<Map<String, Object>> rows) {
            super(null);
            this.rows = rows;
        }

        @Override
        public List<Map<String, Object>> executeQuery(String sql, Object... args) {
            this.lastArgs = args == null ? List.of() : List.of(args);
            return rows;
        }
    }

    private static class DisabledPythonAiChatClient extends PythonAiChatClient {
        DisabledPythonAiChatClient(ChatAnalysisService chatAnalysisService) {
            super(null, chatAnalysisService, "http://localhost:8000", false);
        }

        @Override
        public Optional<ChatResponse> ask(String question, ChatSession session, long startTime) {
            return Optional.empty();
        }
    }
}
