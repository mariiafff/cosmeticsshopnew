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
                new DisabledPythonAiChatClient(chatAnalysisService)
        );
        ChatRequest request = new ChatRequest();
        request.setQuestion("Top selling products");

        ChatResponse response = chatService.ask(request);

        assertEquals("SUCCESS", response.getStatus());
        assertTrue(response.getGeneratedSql().contains("ai_safe.seller_product_sales_summary"));
        assertTrue(response.getGeneratedSql().contains("store_id = ?"));
        assertEquals(List.of(77L), queryExecutionService.lastArgs);
        assertEquals(3, response.getRows().size());
        assertTrue(response.getFinalAnswer().contains("Luna Vitamin C Serum"));
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
