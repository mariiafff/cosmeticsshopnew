package com.cosmeticsshop.service.chatgraph;

import com.cosmeticsshop.dto.ChatResponse;
import com.cosmeticsshop.service.ChatAnalysisService;
import com.cosmeticsshop.service.ChatSessionService.ChatSession;
import com.cosmeticsshop.service.GeminiSqlService;
import com.cosmeticsshop.service.GuardrailsService;
import com.cosmeticsshop.service.QueryExecutionService;
import com.cosmeticsshop.service.SqlSafetyService;
import com.cosmeticsshop.util.SqlWhitelist;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatGraphOrchestratorTest {

    private static final String SAFE_SQL = "SELECT membership_type, avg_spend FROM ai_safe.membership_summary ORDER BY avg_spend DESC LIMIT 1";
    private static final String CITY_SQL = "SELECT city, total_customers AS customer_count FROM ai_safe.city_customer_summary ORDER BY total_customers DESC LIMIT 1";
    private static final String COUNTRY_SQL = "SELECT country, total_revenue FROM ai_safe.country_revenue_summary ORDER BY total_revenue DESC LIMIT 1";
    private static final String SELLER_LATEST_SQL = "SELECT product_name, order_date, quantity, unit_price FROM ai_safe.seller_recent_sold_products WHERE store_id = ? ORDER BY order_date DESC LIMIT 1";
    private static final String SELLER_REVENUE_SQL = "SELECT total_revenue, total_orders, total_items_sold FROM ai_safe.seller_revenue_summary WHERE store_id = ?";
    private static final String SELLER_TOP_PRODUCTS_SQL = "SELECT product_name, total_quantity AS total_sold FROM ai_safe.seller_product_sales_summary WHERE store_id = ? AND total_quantity > 0 ORDER BY total_sold DESC LIMIT 5";
    private static final String USER_LAST_ORDER_SQL = "SELECT order_id, customer_id, order_date, total_amount FROM ai_safe.user_recent_orders WHERE customer_id = ? ORDER BY order_date DESC LIMIT 1";
    private static final String USER_LAST_ORDER_ITEMS_SQL = "SELECT r.order_id, r.order_date, r.total_amount, i.product_name, i.quantity, i.unit_price, (i.quantity * i.unit_price) AS line_total FROM ai_safe.user_order_items i JOIN ai_safe.user_recent_orders r ON r.order_id = i.order_id WHERE r.customer_id = ? AND r.order_id = (SELECT order_id FROM ai_safe.user_recent_orders WHERE customer_id = ? ORDER BY order_date DESC LIMIT 1) ORDER BY i.product_name LIMIT 50";

    private FakeGeminiSqlService geminiSqlService;
    private FakeQueryExecutionService queryExecutionService;
    private ChatGraphOrchestrator orchestrator;
    private ChatSession session;

    @BeforeEach
    void setUp() {
        geminiSqlService = new FakeGeminiSqlService();
        queryExecutionService = new FakeQueryExecutionService();
        ChatAnalysisService chatAnalysisService = new ChatAnalysisService();

        orchestrator = new ChatGraphOrchestrator(
                new QuestionAnalysisNode(),
                new SqlGenerationNode(geminiSqlService),
                new SqlSafetyCheckNode(new SqlSafetyService(new SqlWhitelist())),
                new GuardrailsCheckNode(new GuardrailsService()),
                new QueryExecutionNode(queryExecutionService, chatAnalysisService),
                new ResponseFormattingNode(chatAnalysisService),
                new ErrorHandlingNode()
        );
        session = new ChatSession(true, "ADMIN", "admin@example.com", 1L, null, "127.0.0.1");
    }

    @Test
    void safeSqlPassesThroughGraph() {
        geminiSqlService.thenReturn(SAFE_SQL);
        queryExecutionService.thenReturn(List.of(
                Map.of("membership_type", "Gold", "avg_spend", 120)
        ));

        ChatResponse response = orchestrator.run("which membership type spends the most?", session, System.nanoTime());

        assertEquals("SUCCESS", response.getStatus());
        assertEquals(SAFE_SQL, response.getGeneratedSql());
        assertEquals(1, response.getRows().size());
        assertEquals("Gold is the membership type with the highest average spend: 120.", response.getFinalAnswer());
    }

    @Test
    void membershipQuestionReturnsResultWithCanonicalSql() {
        useRealGeminiSqlService();
        queryExecutionService.thenReturn(List.of(
                Map.of("membership_type", "Gold", "avg_spend", 120)
        ));

        ChatResponse response = orchestrator.run("Which membership type spends the most?", session, System.nanoTime());

        assertEquals("SUCCESS", response.getStatus());
        assertEquals(SAFE_SQL, response.getGeneratedSql());
        assertEquals("Gold is the membership type with the highest average spend: 120.", response.getFinalAnswer());
    }

    @Test
    void cityQuestionReturnsResultWithCanonicalSql() {
        useRealGeminiSqlService();
        queryExecutionService.thenReturn(List.of(
                Map.of("city", "Istanbul", "customer_count", 42)
        ));

        ChatResponse response = orchestrator.run("Which city has the most customers?", session, System.nanoTime());

        assertEquals("SUCCESS", response.getStatus());
        assertEquals(CITY_SQL, response.getGeneratedSql());
        assertEquals("Istanbul has the highest customer count with 42 customers.", response.getFinalAnswer());
    }

    @Test
    void countryQuestionReturnsResultWithCanonicalSql() {
        useRealGeminiSqlService();
        queryExecutionService.thenReturn(List.of(
                Map.of("country", "UK", "total_revenue", 999)
        ));

        ChatResponse response = orchestrator.run("Which country generates the most revenue?", session, System.nanoTime());

        assertEquals("SUCCESS", response.getStatus());
        assertEquals(COUNTRY_SQL, response.getGeneratedSql());
        assertEquals("UK generates the most revenue: 999.", response.getFinalAnswer());
    }

    @Test
    void sellerAsksLatestSoldProductReturnsOnlyOwnStoreProduct() {
        session = new ChatSession(true, "CORPORATE", "seller@test.com", 10L, 77L, "127.0.0.1");
        useRealGeminiSqlService();
        queryExecutionService.thenReturn(List.of(
                Map.of("product_name", "Luna Manager Bright Serum", "quantity", 2, "unit_price", 31.90)
        ));

        ChatResponse response = orchestrator.run("mağazamdan alışveriş yapılan en son ürün nedir", session, System.nanoTime());

        assertEquals("SUCCESS", response.getStatus());
        assertEquals(SELLER_LATEST_SQL, response.getGeneratedSql());
        assertEquals(List.of(77L), queryExecutionService.lastArgs);
        assertTrue(response.getFinalAnswer().contains("Luna Manager Bright Serum"));
    }

    @Test
    void sellerRevenueUsesOwnStoreScope() {
        session = new ChatSession(true, "CORPORATE", "seller@test.com", 10L, 77L, "127.0.0.1");
        useRealGeminiSqlService();
        queryExecutionService.thenReturn(List.of(
                Map.of("total_revenue", 103.30, "total_orders", 1, "total_items_sold", 3)
        ));

        ChatResponse response = orchestrator.run("mağazamın geliri nedir", session, System.nanoTime());

        assertEquals("SUCCESS", response.getStatus());
        assertEquals(SELLER_REVENUE_SQL, response.getGeneratedSql());
        assertEquals(List.of(77L), queryExecutionService.lastArgs);
        assertTrue(response.getFinalAnswer().contains("103.3"));
    }

    @Test
    void sellerTopSellingProductsReturnsNonEmptyStoreScopedResult() {
        session = new ChatSession(true, "CORPORATE", "seller@test.com", 10L, 77L, "127.0.0.1");
        useRealGeminiSqlService();
        queryExecutionService.thenReturn(List.of(
                Map.of("product_name", "Luna Vitamin C Serum", "total_sold", 12),
                Map.of("product_name", "Luna Barrier Repair Cream", "total_sold", 9),
                Map.of("product_name", "Luna Gentle Gel Cleanser", "total_sold", 7)
        ));

        ChatResponse response = orchestrator.run("mağazamda en çok satılan ürün nedir", session, System.nanoTime());

        assertEquals("SUCCESS", response.getStatus());
        assertTrue(response.getGeneratedSql().contains("ai_safe.seller_product_sales_summary"));
        assertTrue(response.getGeneratedSql().contains("store_id = ?"));
        assertEquals(List.of(77L), queryExecutionService.lastArgs);
        assertEquals(3, response.getRows().size());
        assertTrue(response.getFinalAnswer().contains("Luna Vitamin C Serum"));
    }

    @Test
    void sellerCannotQueryOtherStoreData() {
        session = new ChatSession(true, "CORPORATE", "seller@test.com", 10L, 77L, "127.0.0.1");
        geminiSqlService.thenReturn(SELLER_REVENUE_SQL);

        ChatResponse response = orchestrator.run("show all stores revenue", session, System.nanoTime());

        assertEquals("BLOCKED", response.getStatus());
        assertEquals("GUARDRAIL", response.getAgent());
        assertEquals(List.of(), response.getRows());
    }

    @Test
    void sellerUnscopedSellerViewQueryIsBlocked() {
        session = new ChatSession(true, "CORPORATE", "seller@test.com", 10L, 77L, "127.0.0.1");
        geminiSqlService.thenReturn("SELECT product_name FROM ai_safe.seller_recent_sold_products ORDER BY order_date DESC LIMIT 1");

        ChatResponse response = orchestrator.run("mağazamdan alışveriş yapılan en son ürün nedir", session, System.nanoTime());

        assertEquals("ERROR", response.getStatus());
        assertEquals("VALIDATOR", response.getAgent());
        assertTrue(response.getFinalAnswer().contains("violated safety rules"));
    }

    @Test
    void individualCannotAccessSellerAnalytics() {
        session = new ChatSession(true, "INDIVIDUAL", "demo@luime.com", 11L, null, "127.0.0.1");
        useRealGeminiSqlService();

        ChatResponse response = orchestrator.run("mağazamdan alışveriş yapılan en son ürün nedir", session, System.nanoTime());

        assertEquals("BLOCKED", response.getStatus());
        assertEquals("GUARDRAIL", response.getAgent());
    }

    @Test
    void individualLastOrderQueryReturnsOneOwnRow() {
        session = new ChatSession(true, "INDIVIDUAL", "demo@luime.com", 11L, null, "127.0.0.1");
        useRealGeminiSqlService();
        queryExecutionService.thenReturn(List.of(
                Map.of("order_id", 1001, "customer_id", 11, "total_amount", 88.40)
        ));

        ChatResponse response = orchestrator.run("son yaptığım alışveriş nedir", session, System.nanoTime());

        assertEquals("SUCCESS", response.getStatus());
        assertEquals(USER_LAST_ORDER_SQL, response.getGeneratedSql());
        assertEquals(List.of(11L), queryExecutionService.lastArgs);
        assertEquals(1, response.getRows().size());
        assertTrue(response.getFinalAnswer().contains("88.4"));
    }

    @Test
    void individualLastOrderItemsReturnsMultipleOwnRows() {
        session = new ChatSession(true, "INDIVIDUAL", "demo@luime.com", 11L, null, "127.0.0.1");
        useRealGeminiSqlService();
        queryExecutionService.thenReturn(List.of(
                Map.of("order_id", 1001, "total_amount", 119.90, "product_name", "Luna Vitamin C Serum", "quantity", 1, "unit_price", 34.90, "line_total", 34.90),
                Map.of("order_id", 1001, "total_amount", 119.90, "product_name", "Luna Barrier Repair Cream", "quantity", 2, "unit_price", 42.50, "line_total", 85.00)
        ));

        ChatResponse response = orchestrator.run("son alışverişin detayları ve aldığım ürünler", session, System.nanoTime());

        assertEquals("SUCCESS", response.getStatus());
        assertEquals(USER_LAST_ORDER_ITEMS_SQL, response.getGeneratedSql());
        assertEquals(List.of(11L, 11L), queryExecutionService.lastArgs);
        assertEquals(2, response.getRows().size());
        assertTrue(response.getFinalAnswer().contains("119.9"));
        assertTrue(response.getFinalAnswer().contains("Luna Vitamin C Serum"));
        assertTrue(response.getFinalAnswer().contains("Luna Barrier Repair Cream"));
    }

    @Test
    void individualLatestOrderContentsIntentDoesNotReturnOnlySummary() {
        session = new ChatSession(true, "INDIVIDUAL", "demo@luime.com", 11L, null, "127.0.0.1");
        useRealGeminiSqlService();
        queryExecutionService.thenReturn(List.of(
                Map.of("order_id", 1001, "total_amount", 103.30, "product_name", "Product A", "quantity", 2, "unit_price", 20.0, "line_total", 40.0),
                Map.of("order_id", 1001, "total_amount", 103.30, "product_name", "Product B", "quantity", 1, "unit_price", 63.3, "line_total", 63.3)
        ));

        ChatResponse response = orchestrator.run("en son yaptığım alışverişin tutarı ve içerikleri neler", session, System.nanoTime());

        assertEquals("SUCCESS", response.getStatus());
        assertEquals(USER_LAST_ORDER_ITEMS_SQL, response.getGeneratedSql());
        assertEquals(List.of(11L, 11L), queryExecutionService.lastArgs);
        assertEquals(2, response.getRows().size());
        assertTrue(response.getRows().get(0).containsKey("product_name"));
        assertTrue(response.getFinalAnswer().contains("Your latest order total is 103.3. It includes:"));
        assertTrue(response.getFinalAnswer().contains("Product A x2 = 40"));
    }

    @Test
    void individualPercentageQueryReturnsNumericResult() {
        session = new ChatSession(true, "INDIVIDUAL", "demo@luime.com", 11L, null, "127.0.0.1");
        useRealGeminiSqlService();
        queryExecutionService.thenReturn(List.of(
                Map.of("order_id", 1001, "last_order_amount", 50.0, "total_spent", 200.0, "percentage_of_total_spent", 25.0)
        ));

        ChatResponse response = orchestrator.run("son yaptığım alışveriş toplam harcamamın yüzde kaçı", session, System.nanoTime());

        assertEquals("SUCCESS", response.getStatus());
        assertTrue(response.getGeneratedSql().contains("ai_safe.user_order_summary"));
        assertEquals(List.of(11L), queryExecutionService.lastArgs);
        assertEquals(25.0, response.getRows().get(0).get("percentage_of_total_spent"));
        assertTrue(response.getFinalAnswer().contains("25"));
    }

    @Test
    void individualPercentageQueryDbErrorReturnsPartialSupportFallback() {
        session = new ChatSession(true, "INDIVIDUAL", "demo@luime.com", 11L, null, "127.0.0.1");
        useRealGeminiSqlService();
        queryExecutionService.thenThrow(new DataAccessResourceFailureException("ratio failed"));

        ChatResponse response = orchestrator.run("son yaptığım alışveriş toplam harcamamın yüzde kaçı", session, System.nanoTime());

        assertEquals("ERROR", response.getStatus());
        assertEquals("I can show your last order and total spending, but percentage breakdown is not supported yet.", response.getFinalAnswer());
    }

    @Test
    void adminCanAccessAggregateAnalytics() {
        useRealGeminiSqlService();
        queryExecutionService.thenReturn(List.of(
                Map.of("country", "UK", "total_revenue", 999)
        ));

        ChatResponse response = orchestrator.run("Which country generates the most revenue?", session, System.nanoTime());

        assertEquals("SUCCESS", response.getStatus());
        assertEquals(COUNTRY_SQL, response.getGeneratedSql());
    }

    @Test
    void unsafeSqlIsBlockedAfterRetryFails() {
        String unsafeSql = "DELETE FROM ai_safe.orders";
        geminiSqlService.thenReturn(unsafeSql);

        ChatResponse response = orchestrator.run("which membership type spends the most?", session, System.nanoTime());

        assertEquals("ERROR", response.getStatus());
        assertEquals("VALIDATOR", response.getAgent());
        assertNull(response.getGeneratedSql());
        assertEquals(List.of(), response.getRows());
        assertTrue(response.getFinalAnswer().contains("violated safety rules"));
    }

    @Test
    void unsafeSqlTriggersOneRetryAndThenSucceeds() {
        geminiSqlService.thenReturn("SELECT * FROM ai_safe.membership_summary");
        geminiSqlService.thenReturn(SAFE_SQL);
        queryExecutionService.thenReturn(List.of(
                Map.of("membership_type", "Gold", "avg_spend", 120)
        ));

        ChatResponse response = orchestrator.run("which membership type spends the most?", session, System.nanoTime());

        assertEquals("SUCCESS", response.getStatus());
        assertEquals(SAFE_SQL, response.getGeneratedSql());
        assertEquals("Query executed successfully after SQL safety retry.", response.getMessage());
        assertEquals(List.of(false, true), geminiSqlService.strictRetryCalls);
    }

    @Test
    void failedRetryReturnsRefusal() {
        geminiSqlService.thenReturn("SELECT * FROM ai_safe.membership_summary");
        geminiSqlService.thenReturn("SELECT password_hash FROM ai_safe.customer_profiles LIMIT 1");

        ChatResponse response = orchestrator.run("which membership type spends the most?", session, System.nanoTime());

        assertEquals("ERROR", response.getStatus());
        assertEquals("VALIDATOR", response.getAgent());
        assertEquals("Generated SQL did not pass validation.", response.getMessage());
        assertTrue(response.getFinalAnswer().contains("violated safety rules"));
    }

    @Test
    void dbErrorReturnsSafeError() {
        geminiSqlService.thenReturn(SAFE_SQL);
        queryExecutionService.thenThrow(new DataAccessResourceFailureException("connection details"));

        ChatResponse response = orchestrator.run("which membership type spends the most?", session, System.nanoTime());

        assertEquals("ERROR", response.getStatus());
        assertNull(response.getGeneratedSql());
        assertEquals("Analytics query failed due to internal issue.", response.getFinalAnswer());
    }

    @Test
    void emptyResultReturnsFriendlyMessage() {
        geminiSqlService.thenReturn(SAFE_SQL);
        queryExecutionService.thenReturn(List.of());

        ChatResponse response = orchestrator.run("which membership type spends the most?", session, System.nanoTime());

        assertEquals("SUCCESS", response.getStatus());
        assertEquals(SAFE_SQL, response.getGeneratedSql());
        assertTrue(response.getFinalAnswer().contains("No data found"));
    }

    @Test
    void endpointResponseShapeRemainsBackwardCompatible() {
        geminiSqlService.thenReturn(SAFE_SQL);
        queryExecutionService.thenReturn(List.of(
                Map.of("membership_type", "Gold", "avg_spend", 120)
        ));

        ChatResponse response = orchestrator.run("which membership type spends the most?", session, System.nanoTime());

        assertEquals("which membership type spends the most?", response.getQuestion());
        assertEquals(SAFE_SQL, response.getGeneratedSql());
        assertEquals("Query executed successfully.", response.getMessage());
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("ANALYSIS", response.getAgent());
        assertEquals("BAR", response.getVisualizationType());
        assertTrue(response.getSecurityDetails().containsKey("role"));
        assertTrue(response.getSteps().contains("Generate SQL"));
    }

    private static class FakeGeminiSqlService extends GeminiSqlService {

        private final Queue<String> sqlResponses = new ArrayDeque<>();
        private final List<Boolean> strictRetryCalls = new ArrayList<>();

        FakeGeminiSqlService() {
            super(null, "test-key", "http://localhost");
        }

        void thenReturn(String sql) {
            sqlResponses.add(sql);
        }

        @Override
        public String generateSql(String question, String role, Long storeId, boolean strictRetry) {
            strictRetryCalls.add(strictRetry);
            if (sqlResponses.size() == 1) {
                return sqlResponses.peek();
            }
            return sqlResponses.remove();
        }
    }

    private void useRealGeminiSqlService() {
        ChatAnalysisService chatAnalysisService = new ChatAnalysisService();
        orchestrator = new ChatGraphOrchestrator(
                new QuestionAnalysisNode(),
                new SqlGenerationNode(new GeminiSqlService(null, "test-key", "http://localhost")),
                new SqlSafetyCheckNode(new SqlSafetyService(new SqlWhitelist())),
                new GuardrailsCheckNode(new GuardrailsService()),
                new QueryExecutionNode(queryExecutionService, chatAnalysisService),
                new ResponseFormattingNode(chatAnalysisService),
                new ErrorHandlingNode()
        );
    }

    private static class FakeQueryExecutionService extends QueryExecutionService {

        private List<Map<String, Object>> rows = List.of();
        private RuntimeException exception;

        FakeQueryExecutionService() {
            super(null);
        }

        void thenReturn(List<Map<String, Object>> rows) {
            this.rows = rows;
            this.exception = null;
        }

        void thenThrow(RuntimeException exception) {
            this.exception = exception;
        }

        @Override
        public List<Map<String, Object>> executeQuery(String sql) {
            this.lastSql = sql;
            this.lastArgs = List.of();
            if (exception != null) {
                throw exception;
            }
            return rows;
        }

        private String lastSql;
        private List<Object> lastArgs = List.of();

        @Override
        public List<Map<String, Object>> executeQuery(String sql, Object... args) {
            this.lastSql = sql;
            this.lastArgs = args == null ? List.of() : List.of(args);
            if (exception != null) {
                throw exception;
            }
            return rows;
        }
    }
}
