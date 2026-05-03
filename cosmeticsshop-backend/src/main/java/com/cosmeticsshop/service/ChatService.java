package com.cosmeticsshop.service;

import com.cosmeticsshop.dto.ChatRequest;
import com.cosmeticsshop.dto.ChatResponse;
import com.cosmeticsshop.dto.GuardrailResult;
import com.cosmeticsshop.service.ChatAnalysisService.VisualizationPayload;
import com.cosmeticsshop.service.ChatSessionService.ChatSession;
import com.cosmeticsshop.service.chatgraph.ChatGraphOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final int DEFAULT_REVIEWED_PRODUCTS_LIMIT = 5;
    private static final int MAX_REQUESTED_LIMIT = 100;
    private static final List<Pattern> REQUESTED_LIMIT_PATTERNS = List.of(
            Pattern.compile("\\btop\\s+(\\d{1,3})\\b"),
            Pattern.compile("\\bfirst\\s+(\\d{1,3})\\b"),
            Pattern.compile("\\bbest\\s+(\\d{1,3})\\b"),
            Pattern.compile("\\bshow\\s+(?:me\\s+)?(\\d{1,3})\\b"),
            Pattern.compile("\\blist\\s+(?:the\\s+)?(\\d{1,3})\\b"),
            Pattern.compile("\\b(\\d{1,3})\\s+(?:urun|urunu|urunler|urunleri|products?|items?)\\b")
    );
    private static final Map<String, Integer> NUMBER_WORDS = Map.ofEntries(
            Map.entry("bir", 1),
            Map.entry("one", 1),
            Map.entry("iki", 2),
            Map.entry("two", 2),
            Map.entry("uc", 3),
            Map.entry("three", 3),
            Map.entry("dort", 4),
            Map.entry("four", 4),
            Map.entry("bes", 5),
            Map.entry("five", 5),
            Map.entry("alti", 6),
            Map.entry("six", 6),
            Map.entry("yedi", 7),
            Map.entry("seven", 7),
            Map.entry("sekiz", 8),
            Map.entry("eight", 8),
            Map.entry("dokuz", 9),
            Map.entry("nine", 9),
            Map.entry("on", 10),
            Map.entry("ten", 10)
    );

    private static final List<String> PIPELINE_STEPS = List.of(
            "Ask question",
            "Generate SQL",
            "Validate SQL",
            "Run query",
            "Show results"
    );
    private static final String TOP_PRODUCTS_BY_STORE_SQL = """
            select
                product_name,
                total_quantity as total_sold
            from ai_safe.seller_product_sales_summary
            where store_id = ?
              and total_quantity > 0
            order by total_sold desc
            limit ?
            """;
    private static final String TOP_PRODUCTS_GLOBAL_SQL = """
            select
                p.name as product_name,
                sum(oi.quantity) as total_sold
            from order_items oi
            join products p on p.id = oi.product_id
            join orders o on o.id = oi.order_id
            group by p.id, p.name
            order by total_sold desc
            limit ?
            """;
    private static final String CURRENT_MONTH_CUSTOMER_TOP_CATEGORY_SQL = """
            select
                coalesce(c.name, concat('Kategori ', p.category_id), 'Diğer') as category_name,
                p.category_id,
                sum(oi.quantity * oi.unit_price) as total_spent
            from order_items oi
            join products p on p.id = oi.product_id
            left join categories c on c.id = p.category_id
            join orders o on o.id = oi.order_id
            where o.user_id = ?
              and extract(month from o.created_at) = extract(month from current_date)
              and extract(year from o.created_at) = extract(year from current_date)
            group by c.id, c.name, p.category_id
            order by total_spent desc
            limit 1
            """;
    private static final String LATEST_CREDIT_CARD_ORDER_PRODUCTS_SQL = """
            select
                p.name as product_name,
                oi.quantity,
                oi.unit_price,
                oi.quantity * oi.unit_price as line_total
            from order_items oi
            join products p on p.id = oi.product_id
            join orders o on o.id = oi.order_id
            where o.user_id = ?
              and lower(o.payment_method) in ('card', 'credit_card', 'credit card', 'kredi kartı', 'kredi karti')
              and o.id = (
                  select o2.id
                  from orders o2
                  where o2.user_id = ?
                    and lower(o2.payment_method) in ('card', 'credit_card', 'credit card', 'kredi kartı', 'kredi karti')
                  order by o2.created_at desc, o2.id desc
                  limit 1
              )
            order by oi.id
            """;
    private static final String LATEST_CREDIT_CARD_ORDER_EXISTS_SQL = """
            select count(*) as order_count
            from orders o
            where o.user_id = ?
              and lower(o.payment_method) in ('card', 'credit_card', 'credit card', 'kredi kartı', 'kredi karti')
            """;
    private static final String LAST_PRODUCTS_CATEGORY_DISTRIBUTION_SQL = """
            select
                case
                    when c.name is not null then c.name
                    when p.category_id is not null then concat('Kategori ', p.category_id)
                    else 'Diğer'
                end as category_name,
                p.category_id,
                count(*) as product_count
            from (
                select oi.product_id
                from order_items oi
                join orders o on o.id = oi.order_id
                where o.user_id = ?
                  and o.id = (
                      select id
                      from orders
                      where user_id = ?
                      order by created_at desc, id desc
                      limit 1
                  )
                order by oi.id desc
                limit ?
            ) recent_items
            join products p on p.id = recent_items.product_id
            left join categories c on c.id = p.category_id
            group by c.id, c.name, p.category_id
            order by product_count desc
            """;
    private static final String CUSTOMER_ORDER_EXISTS_SQL = """
            select count(*) as order_count
            from orders o
            where o.user_id = ?
            """;
    private static final String TOP_REVIEWED_PRODUCTS_SQL = """
            select
                p.name as product_name,
                round(cast(avg(r.star_rating) as numeric), 2) as average_rating,
                count(r.id) as review_count
            from reviews r
            join products p on p.id = r.product_id
            group by p.id, p.name
            having count(r.id) > 0
            order by average_rating desc, review_count desc
            limit ?
            """;
    private static final String TOP_REVIEWED_PRODUCTS_BY_STORE_SQL = """
            select
                p.name as product_name,
                round(cast(avg(r.star_rating) as numeric), 2) as average_rating,
                count(r.id) as review_count
            from reviews r
            join products p on p.id = r.product_id
            where p.store_id = ?
            group by p.id, p.name
            having count(r.id) > 0
            order by average_rating desc, review_count desc
            limit ?
            """;
    private static final String LOWEST_RATED_PRODUCTS_SQL = """
            select
                p.name as product_name,
                round(cast(avg(r.star_rating) as numeric), 2) as average_rating,
                count(r.id) as review_count
            from reviews r
            join products p on p.id = r.product_id
            group by p.id, p.name
            having count(r.id) > 0
            order by average_rating asc, review_count desc
            limit 2
            """;
    private static final String LOWEST_RATED_PRODUCTS_BY_STORE_SQL = """
            select
                p.name as product_name,
                round(cast(avg(r.star_rating) as numeric), 2) as average_rating,
                count(r.id) as review_count
            from reviews r
            join products p on p.id = r.product_id
            where p.store_id = ?
            group by p.id, p.name
            having count(r.id) > 0
            order by average_rating asc, review_count desc
            limit 2
            """;

    private final QueryExecutionService queryExecutionService;
    private final GuardrailsService guardrailsService;
    private final ChatSessionService chatSessionService;
    private final ChatRateLimitService chatRateLimitService;
    private final ChatAnalysisService chatAnalysisService;
    private final ChatGraphOrchestrator chatGraphOrchestrator;
    private final PythonAiChatClient pythonAiChatClient;

    public ChatService(
            QueryExecutionService queryExecutionService,
            GuardrailsService guardrailsService,
            ChatSessionService chatSessionService,
            ChatRateLimitService chatRateLimitService,
            ChatAnalysisService chatAnalysisService,
            ChatGraphOrchestrator chatGraphOrchestrator,
            PythonAiChatClient pythonAiChatClient
    ) {
        this.queryExecutionService = queryExecutionService;
        this.guardrailsService = guardrailsService;
        this.chatSessionService = chatSessionService;
        this.chatRateLimitService = chatRateLimitService;
        this.chatAnalysisService = chatAnalysisService;
        this.chatGraphOrchestrator = chatGraphOrchestrator;
        this.pythonAiChatClient = pythonAiChatClient;
    }

    public ChatResponse ask(ChatRequest request) {
        if (request == null || request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
            throw new IllegalArgumentException("Question is required.");
        }

        long startTime = System.nanoTime();
        String question = request.getQuestion().trim();
        ChatSession session = chatSessionService.resolveSession();

        if (!chatRateLimitService.allow(rateLimitKey(session))) {
            return buildBlockedResponse(
                    question,
                    "Rate limit protection triggered.",
                    GuardrailResult.block(
                            "Çok Fazla İstek",
                            "HIGH",
                            "Dakika başına istek limiti aşıldı.",
                            "Rate limit / enumeration protection",
                            "İstek geçici olarak durduruldu",
                            "Bir dakika sonra tekrar deneyebilirsiniz."
                    ),
                    session,
                    startTime
            );
        }

        GuardrailResult guardrailResult = guardrailsService.inspect(question, session);
        if ("GREETING".equals(guardrailResult.getCategory())) {
            return new ChatResponse(
                    chatAnalysisService.escapeHtml(question),
                    null,
                    List.of(),
                    "Greeting recognized.",
                    elapsedMs(startTime),
                    "SUCCESS",
                    "INTENT",
                    "Greeting",
                    null,
                    buildSessionDetails(session),
                    chatAnalysisService.escapeHtml(guardrailResult.getSafeAlternative()),
                    "NONE",
                    Map.of(),
                    List.of("Identify intent", "Greet user")
            );
        }

        if (!guardrailResult.isAllowed() || "OUT_OF_SCOPE".equals(guardrailResult.getCategory())) {
            return buildBlockedResponse(question, guardrailResult.getReason(), guardrailResult, session, startTime);
        }

        if (isCurrentMonthTopProductsIntent(question)) {
            return answerCurrentMonthTopProducts(question, session, startTime);
        }

        if (isLatestCreditCardOrderProductsIntent(question)) {
            return answerLatestCreditCardOrderProducts(question, session, startTime);
        }

        if (isCurrentMonthCustomerTopCategoryIntent(question)) {
            return answerCurrentMonthCustomerTopCategory(question, session, startTime);
        }

        if (isLastProductsCategoryDistributionIntent(question)) {
            return answerLastProductsCategoryDistribution(question, session, startTime);
        }

        if (isLowestRatedProductsIntent(question)) {
            return answerLowestRatedProducts(question, session, startTime);
        }

        if (isTopReviewedProductsIntent(question)) {
            return answerTopReviewedProducts(question, session, startTime);
        }

        return pythonAiChatClient.ask(question, session, startTime)
                .orElseGet(() -> chatGraphOrchestrator.run(question, session, startTime));
    }

    private ChatResponse buildBlockedResponse(
            String question,
            String message,
            GuardrailResult guardrailResult,
            ChatSession session,
            long startTime
    ) {
        Map<String, Object> securityDetails = new LinkedHashMap<>();
        securityDetails.put("role", session.role());
        if (session.storeId() != null) {
            securityDetails.put("sessionStoreId", session.storeId());
        }
        if (session.userId() != null) {
            securityDetails.put("sessionUserId", session.userId());
        }
        securityDetails.put("severity", guardrailResult.getSeverity());
        securityDetails.put("reason", guardrailResult.getReason());
        securityDetails.put("detectionType", guardrailResult.getDetectionType());
        securityDetails.put("blockedAction", guardrailResult.getBlockedAction());
        if (guardrailResult.getSafeAlternative() != null) {
            securityDetails.put("safeAlternative", guardrailResult.getSafeAlternative());
        }

        return new ChatResponse(
                chatAnalysisService.escapeHtml(question),
                null,
                List.of(),
                message == null ? "This request was blocked by security guardrails." : message,
                elapsedMs(startTime),
                "BLOCKED",
                "GUARDRAIL",
                guardrailResult.getDetectionType(),
                guardrailResult.getCategory(),
                securityDetails,
                chatAnalysisService.escapeHtml(
                        guardrailResult.getSafeAlternative() != null
                                ? guardrailResult.getSafeAlternative()
                                : "Bu isteği güvenlik nedeniyle gerçekleştiremiyorum."
                ),
                "NONE",
                Map.of(),
                PIPELINE_STEPS
        );
    }

    private Map<String, Object> buildSessionDetails(ChatSession session) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("role", session.role());
        details.put("guardrail", "Açık");
        if (session.storeId() != null) {
            details.put("storeId", session.storeId());
        }
        if (session.userId() != null) {
            details.put("userId", session.userId());
        }
        return details;
    }

    private boolean isCurrentMonthTopProductsIntent(String question) {
        String normalized = normalizeIntentText(question);
        boolean asksTopSelling =
                normalized.contains("en cok satan")
                        || normalized.contains("en cok satilan")
                        || normalized.contains("top selling")
                        || normalized.contains("best selling")
                        || normalized.contains("top 5 urun")
                        || normalized.contains("top urun")
                        || normalized.matches(".*\\btop\\s+\\d{1,3}\\s+urun\\b.*")
                        || normalized.matches(".*\\btop\\s+\\d{1,3}\\s+products?\\b.*")
                        || normalized.contains("magazamda en cok satan");
        boolean asksProduct = normalized.contains("urun") || normalized.contains("product");
        return asksTopSelling && asksProduct;
    }

    private boolean isLatestCreditCardOrderProductsIntent(String question) {
        String normalized = normalizeIntentText(question);
        boolean asksCreditCard = normalized.contains("kredi kart") || normalized.contains("credit card");
        boolean asksLatest = normalized.contains("en son") || normalized.contains("son ") || normalized.contains("last") || normalized.contains("latest");
        boolean asksOrderOrPurchase = normalized.contains("alisveris") || normalized.contains("siparis") || normalized.contains("order");
        boolean asksProducts = normalized.contains("urun") || normalized.contains("ne aldim") || normalized.contains("product");
        return asksCreditCard && asksLatest && asksOrderOrPurchase && asksProducts;
    }

    private boolean isCurrentMonthCustomerTopCategoryIntent(String question) {
        String normalized = normalizeIntentText(question);
        boolean asksCategory = normalized.contains("kategori") || normalized.contains("category");
        boolean asksSpending = normalized.contains("harcama") || normalized.contains("harcadim") || normalized.contains("spent") || normalized.contains("spending");
        boolean asksTop = normalized.contains("en fazla") || normalized.contains("daha cok") || normalized.contains("most") || normalized.contains("top");
        boolean asksCurrentMonth = normalized.contains("bu ay") || normalized.contains("bu ayki") || normalized.contains("this month");
        return asksCategory && asksSpending && asksTop && asksCurrentMonth;
    }

    private boolean isLastProductsCategoryDistributionIntent(String question) {
        String normalized = normalizeIntentText(question);
        boolean asksRecentPurchase =
                normalized.contains("son aldigim")
                        || normalized.contains("son 5 urun")
                        || normalized.contains("last purchases")
                        || normalized.contains("last purchased")
                        || normalized.contains("recent purchases");
        boolean asksCategoryDistribution =
                normalized.contains("kategori bazli dagilim")
                        || normalized.contains("kategorik bazli dagilim")
                        || normalized.contains("kategorilere gore dagilim")
                        || normalized.contains("category distribution")
                        || (normalized.contains("kategori") && normalized.contains("dagilim"));
        boolean asksProducts = normalized.contains("urun") || normalized.contains("product") || normalized.contains("purchase");
        return asksRecentPurchase && asksCategoryDistribution && asksProducts;
    }

    private boolean isTopReviewedProductsIntent(String question) {
        String normalized = normalizeIntentText(question);
        boolean asksProduct = normalized.contains("urun") || normalized.contains("product");
        boolean asksReviewOrRating =
                normalized.contains("yorumlanmis")
                        || normalized.contains("puanlanmis")
                        || normalized.contains("puanli")
                        || normalized.contains("puan")
                        || normalized.contains("rating")
                        || normalized.contains("reviewed")
                        || normalized.contains("rated");
        boolean asksBest =
                normalized.contains("en iyi")
                        || normalized.contains("en yuksek")
                        || normalized.contains("top")
                        || normalized.contains("best")
                        || normalized.contains("highest");
        return asksProduct && asksReviewOrRating && asksBest;
    }

    private boolean isLowestRatedProductsIntent(String question) {
        String normalized = normalizeIntentText(question);
        boolean asksProduct = normalized.contains("urun") || normalized.contains("product");
        boolean asksReviewOrRating =
                normalized.contains("yorumlanmis")
                        || normalized.contains("puanli")
                        || normalized.contains("puan")
                        || normalized.contains("rating")
                        || normalized.contains("reviewed")
                        || normalized.contains("rated");
        boolean asksWorst =
                normalized.contains("en kotu")
                        || normalized.contains("en dusuk")
                        || normalized.contains("lowest")
                        || normalized.contains("worst");
        return asksProduct && asksReviewOrRating && asksWorst;
    }

    private ChatResponse answerCurrentMonthTopProducts(String question, ChatSession session, long startTime) {
        boolean isCorporate = "CORPORATE".equals(session.role());
        if (isCorporate && session.storeId() == null) {
            return new ChatResponse(
                    chatAnalysisService.escapeHtml(question),
                    null,
                    List.of(),
                    "No store is linked to this seller session.",
                    elapsedMs(startTime),
                    "SUCCESS",
                    "ANALYSIS",
                    null,
                    null,
                    buildSessionDetails(session),
                    "Bu hesap için bağlı mağaza bulunamadı.",
                    "NONE",
                    Map.of(),
                    PIPELINE_STEPS
            );
        }

        int requestedLimit = extractRequestedLimit(question, 5);
        String sql = isCorporate ? TOP_PRODUCTS_BY_STORE_SQL : TOP_PRODUCTS_GLOBAL_SQL;
        List<Map<String, Object>> rows = chatAnalysisService.sanitizeRows(
                isCorporate
                        ? queryExecutionService.executeQuery(sql, session.storeId(), requestedLimit)
                        : queryExecutionService.executeQuery(sql, requestedLimit)
        );
        VisualizationPayload visualization = chatAnalysisService.buildVisualization(rows);
        String displaySql = isCorporate
                ? sql.replaceFirst("\\?", String.valueOf(session.storeId())).replaceFirst("\\?", String.valueOf(requestedLimit))
                : sql.replaceFirst("\\?", String.valueOf(requestedLimit));

        return new ChatResponse(
                chatAnalysisService.escapeHtml(question),
                chatAnalysisService.escapeHtml(displaySql),
                rows,
                "Top-selling products query executed successfully.",
                elapsedMs(startTime),
                "SUCCESS",
                "ANALYSIS",
                null,
                null,
                buildSessionDetails(session),
                chatAnalysisService.escapeHtml(buildTopProductsAnswer(rows, isCorporate, requestedLimit)),
                visualization.visualizationType(),
                visualization.chartData(),
                PIPELINE_STEPS
        );
    }

    private String buildTopProductsAnswer(List<Map<String, Object>> rows, boolean scopedToStore, int requestedLimit) {
        if (rows == null || rows.isEmpty()) {
            return "Satış verisi bulunamadı.";
        }

        StringBuilder answer = new StringBuilder();
        answer.append(scopedToStore ? "Mağazanızın en çok satan " : "En çok satan ")
                .append(requestedLimit)
                .append(" ürünü:");

        for (int index = 0; index < rows.size(); index++) {
            Map<String, Object> row = rows.get(index);
            answer.append("\n")
                    .append(index + 1)
                    .append(". ")
                    .append(row.get("product_name"))
                    .append(" - ")
                    .append(row.get("total_sold"))
                    .append(" adet");
        }

        return answer.toString();
    }

    private ChatResponse answerLatestCreditCardOrderProducts(String question, ChatSession session, long startTime) {
        if (!"INDIVIDUAL".equals(session.role())) {
            return new ChatResponse(
                    chatAnalysisService.escapeHtml(question),
                    null,
                    List.of(),
                    "This intent is only available for customer accounts.",
                    elapsedMs(startTime),
                    "BLOCKED",
                    "GUARDRAIL",
                    "Role scope restriction",
                    "Yetki Kapsamı",
                    buildSessionDetails(session),
                    "Bu soru yalnızca müşteri hesabının kendi siparişleri için yanıtlanabilir.",
                    "NONE",
                    Map.of(),
                    PIPELINE_STEPS
            );
        }

        if (session.userId() == null) {
            return new ChatResponse(
                    chatAnalysisService.escapeHtml(question),
                    null,
                    List.of(),
                    "Authentication is required for customer order questions.",
                    elapsedMs(startTime),
                    "BLOCKED",
                    "GUARDRAIL",
                    "Authentication required",
                    "Kimlik Doğrulama Gerekli",
                    buildSessionDetails(session),
                    "Siparişlerinizi görebilmem için giriş yapmanız gerekir.",
                    "NONE",
                    Map.of(),
                    PIPELINE_STEPS
            );
        }

        List<Map<String, Object>> rows = chatAnalysisService.sanitizeRows(
                queryExecutionService.executeQuery(LATEST_CREDIT_CARD_ORDER_PRODUCTS_SQL, session.userId(), session.userId())
        );
        VisualizationPayload visualization = chatAnalysisService.buildVisualization(rows);

        return new ChatResponse(
                chatAnalysisService.escapeHtml(question),
                chatAnalysisService.escapeHtml(LATEST_CREDIT_CARD_ORDER_PRODUCTS_SQL),
                rows,
                "Latest credit-card order products query executed successfully.",
                elapsedMs(startTime),
                "SUCCESS",
                "ANALYSIS",
                null,
                null,
                buildSessionDetails(session),
                chatAnalysisService.escapeHtml(buildLatestCreditCardOrderAnswer(rows, session.userId())),
                visualization.visualizationType(),
                visualization.chartData(),
                PIPELINE_STEPS
        );
    }

    private String buildLatestCreditCardOrderAnswer(List<Map<String, Object>> rows, Long userId) {
        if (rows == null || rows.isEmpty()) {
            boolean hasCreditCardOrder = hasCreditCardOrder(userId);
            return hasCreditCardOrder
                    ? "Son kredi kartı alışverişinizde ürün bulunamadı."
                    : "Kredi kartı ile yapılmış bir alışverişiniz bulunamadı.";
        }

        StringBuilder answer = new StringBuilder("Son kredi kartı alışverişinizdeki ürünler:");
        for (int index = 0; index < rows.size(); index++) {
            Map<String, Object> row = rows.get(index);
            answer.append("\n")
                    .append(index + 1)
                    .append(". ")
                    .append(row.get("product_name"))
                    .append(" - ")
                    .append(row.get("quantity"))
                    .append(" adet, birim fiyat: ")
                    .append(row.get("unit_price"))
                    .append(", toplam: ")
                    .append(row.get("line_total"));
        }
        return answer.toString();
    }

    private boolean hasCreditCardOrder(Long userId) {
        List<Map<String, Object>> rows = queryExecutionService.executeQuery(LATEST_CREDIT_CARD_ORDER_EXISTS_SQL, userId);
        if (rows.isEmpty()) {
            return false;
        }
        Object count = rows.get(0).get("order_count");
        return count instanceof Number number && number.longValue() > 0;
    }

    private boolean hasAnyOrder(Long userId) {
        List<Map<String, Object>> rows = queryExecutionService.executeQuery(CUSTOMER_ORDER_EXISTS_SQL, userId);
        if (rows.isEmpty()) {
            return false;
        }
        Object count = rows.get(0).get("order_count");
        return count instanceof Number number && number.longValue() > 0;
    }

    private ChatResponse answerCurrentMonthCustomerTopCategory(String question, ChatSession session, long startTime) {
        if (!"INDIVIDUAL".equals(session.role())) {
            return new ChatResponse(
                    chatAnalysisService.escapeHtml(question),
                    null,
                    List.of(),
                    "This intent is only available for customer accounts.",
                    elapsedMs(startTime),
                    "BLOCKED",
                    "GUARDRAIL",
                    "Role scope restriction",
                    "Yetki Kapsamı",
                    buildSessionDetails(session),
                    "Bu soru yalnızca müşteri hesabının kendi harcamaları için yanıtlanabilir.",
                    "NONE",
                    Map.of(),
                    PIPELINE_STEPS
            );
        }

        if (session.userId() == null) {
            return new ChatResponse(
                    chatAnalysisService.escapeHtml(question),
                    null,
                    List.of(),
                    "Authentication is required for customer spending questions.",
                    elapsedMs(startTime),
                    "BLOCKED",
                    "GUARDRAIL",
                    "Authentication required",
                    "Kimlik Doğrulama Gerekli",
                    buildSessionDetails(session),
                    "Harcamalarınızı görebilmem için giriş yapmanız gerekir.",
                    "NONE",
                    Map.of(),
                    PIPELINE_STEPS
            );
        }

        List<Map<String, Object>> rows = chatAnalysisService.sanitizeRows(
                queryExecutionService.executeQuery(CURRENT_MONTH_CUSTOMER_TOP_CATEGORY_SQL, session.userId())
        );
        VisualizationPayload visualization = buildCurrentMonthTopCategoryVisualization(rows);

        return new ChatResponse(
                chatAnalysisService.escapeHtml(question),
                chatAnalysisService.escapeHtml(CURRENT_MONTH_CUSTOMER_TOP_CATEGORY_SQL),
                rows,
                "Current-month customer category spending query executed successfully.",
                elapsedMs(startTime),
                "SUCCESS",
                "ANALYSIS",
                null,
                null,
                buildSessionDetails(session),
                chatAnalysisService.escapeHtml(buildCurrentMonthTopCategoryAnswer(rows)),
                visualization.visualizationType(),
                visualization.chartData(),
                PIPELINE_STEPS
        );
    }

    private String buildCurrentMonthTopCategoryAnswer(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return "Bu ay için harcama verisi bulunamadı.";
        }

        Map<String, Object> row = rows.get(0);
        Object categoryName = row.get("category_name");
        if (categoryName == null || String.valueOf(categoryName).isBlank()) {
            categoryName = row.get("category_id");
        }
        return "Bu ay en fazla harcama yaptığınız kategori: "
                + categoryName
                + " (Toplam: "
                + row.get("total_spent")
                + " TL)";
    }

    private VisualizationPayload buildCurrentMonthTopCategoryVisualization(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return new VisualizationPayload("NONE", Map.of());
        }

        Map<String, Object> row = rows.get(0);
        Object categoryName = row.get("category_name");
        Object totalSpent = row.get("total_spent");
        if (categoryName == null || totalSpent == null) {
            return new VisualizationPayload("TABLE", Map.of());
        }

        Map<String, Object> chartData = new LinkedHashMap<>();
        chartData.put("labels", List.of(String.valueOf(categoryName)));
        chartData.put("values", List.of(totalSpent));
        chartData.put("type", "bar");
        return new VisualizationPayload("BAR", chartData);
    }

    private ChatResponse answerLastProductsCategoryDistribution(String question, ChatSession session, long startTime) {
        if (!"INDIVIDUAL".equals(session.role())) {
            return new ChatResponse(
                    chatAnalysisService.escapeHtml(question),
                    null,
                    List.of(),
                    "This intent is only available for customer accounts.",
                    elapsedMs(startTime),
                    "BLOCKED",
                    "GUARDRAIL",
                    "Role scope restriction",
                    "Yetki Kapsamı",
                    buildSessionDetails(session),
                    "Bu soru yalnızca müşteri hesabının kendi alışverişleri için yanıtlanabilir.",
                    "NONE",
                    Map.of(),
                    PIPELINE_STEPS
            );
        }

        if (session.userId() == null) {
            return new ChatResponse(
                    chatAnalysisService.escapeHtml(question),
                    null,
                    List.of(),
                    "Authentication is required for customer purchase questions.",
                    elapsedMs(startTime),
                    "BLOCKED",
                    "GUARDRAIL",
                    "Authentication required",
                    "Kimlik Doğrulama Gerekli",
                    buildSessionDetails(session),
                    "Alışverişlerinizi görebilmem için giriş yapmanız gerekir.",
                    "NONE",
                    Map.of(),
                    PIPELINE_STEPS
            );
        }

        int requestedLimit = extractRequestedLimit(question, 5);
        List<Map<String, Object>> rows = chatAnalysisService.sanitizeRows(
                queryExecutionService.executeQuery(LAST_PRODUCTS_CATEGORY_DISTRIBUTION_SQL, session.userId(), session.userId(), requestedLimit)
        );
        VisualizationPayload visualization = chatAnalysisService.buildVisualization(rows);
        String displaySql = LAST_PRODUCTS_CATEGORY_DISTRIBUTION_SQL.replaceFirst("\\?", String.valueOf(session.userId()))
                .replaceFirst("\\?", String.valueOf(session.userId()))
                .replaceFirst("\\?", String.valueOf(requestedLimit));

        return new ChatResponse(
                chatAnalysisService.escapeHtml(question),
                chatAnalysisService.escapeHtml(displaySql),
                rows,
                "Last purchased products category distribution query executed successfully.",
                elapsedMs(startTime),
                "SUCCESS",
                "ANALYSIS",
                null,
                null,
                buildSessionDetails(session),
                chatAnalysisService.escapeHtml(buildLastProductsCategoryDistributionAnswer(rows, session.userId(), requestedLimit)),
                visualization.visualizationType(),
                visualization.chartData(),
                PIPELINE_STEPS
        );
    }

    private String buildLastProductsCategoryDistributionAnswer(List<Map<String, Object>> rows, Long userId, int requestedLimit) {
        if (rows == null || rows.isEmpty()) {
            return hasAnyOrder(userId)
                    ? "Son " + requestedLimit + " ürün için kategori verisi bulunamadı."
                    : "Henüz alışveriş veriniz bulunamadı.";
        }

        StringBuilder answer = new StringBuilder("Son ")
                .append(requestedLimit)
                .append(" ürününüzün kategori dağılımı:");
        for (Map<String, Object> row : rows) {
            Object categoryName = row.get("category_name");
            if (categoryName == null || String.valueOf(categoryName).isBlank()) {
                categoryName = row.get("category_id");
            }
            answer.append("\n- ")
                    .append(categoryName)
                    .append(": ")
                    .append(row.get("product_count"))
                    .append(" ürün");
        }
        return answer.toString();
    }

    private ChatResponse answerTopReviewedProducts(String question, ChatSession session, long startTime) {
        boolean isCorporate = "CORPORATE".equals(session.role());
        if (isCorporate && session.storeId() == null) {
            return new ChatResponse(
                    chatAnalysisService.escapeHtml(question),
                    null,
                    List.of(),
                    "No store is linked to this seller session.",
                    elapsedMs(startTime),
                    "SUCCESS",
                    "ANALYSIS",
                    null,
                    null,
                    buildSessionDetails(session),
                    "Bu hesap için bağlı mağaza bulunamadı.",
                    "NONE",
                    Map.of(),
                    PIPELINE_STEPS
            );
        }

        int requestedLimit = extractRequestedLimit(question, DEFAULT_REVIEWED_PRODUCTS_LIMIT);
        String sql = isCorporate ? TOP_REVIEWED_PRODUCTS_BY_STORE_SQL : TOP_REVIEWED_PRODUCTS_SQL;
        String displaySql = buildReviewedProductsDisplaySql(sql, isCorporate ? session.storeId() : null, requestedLimit);
        List<Map<String, Object>> rows;
        try {
            rows = chatAnalysisService.sanitizeRows(
                    isCorporate
                            ? queryExecutionService.executeQuery(sql, session.storeId(), requestedLimit)
                            : queryExecutionService.executeQuery(sql, requestedLimit)
            );
            log.info(
                    "top_reviewed_products originalQuestion=\"{}\" extractedLimit={} generatedSql=\"{}\" rowCount={}",
                    question,
                    requestedLimit,
                    displaySql,
                    rows.size()
            );
        } catch (DataAccessException dataAccessException) {
            log.warn(
                    "top_reviewed_products failed originalQuestion=\"{}\" extractedLimit={} generatedSql=\"{}\"",
                    question,
                    requestedLimit,
                    displaySql,
                    dataAccessException
            );
            return new ChatResponse(
                    chatAnalysisService.escapeHtml(question),
                    null,
                    List.of(),
                    "Review analytics query could not be executed.",
                    elapsedMs(startTime),
                    "ERROR",
                    "ANALYSIS",
                    null,
                    null,
                    buildSessionDetails(session),
                    "Şu anda yorum analizi yapılamıyor, lütfen tekrar deneyin.",
                    "NONE",
                    Map.of(),
                    PIPELINE_STEPS
            );
        }
        VisualizationPayload visualization = chatAnalysisService.buildVisualization(rows);

        return new ChatResponse(
                chatAnalysisService.escapeHtml(question),
                chatAnalysisService.escapeHtml(displaySql),
                rows,
                "Top reviewed products query executed successfully.",
                elapsedMs(startTime),
                "SUCCESS",
                "ANALYSIS",
                null,
                null,
                buildSessionDetails(session),
                chatAnalysisService.escapeHtml(buildTopReviewedProductsAnswer(rows, requestedLimit)),
                visualization.visualizationType(),
                visualization.chartData(),
                PIPELINE_STEPS
        );
    }

    private String buildTopReviewedProductsAnswer(List<Map<String, Object>> rows, int requestedLimit) {
        if (rows == null || rows.isEmpty()) {
            return "Henüz yorum verisi bulunamadı.";
        }

        StringBuilder answer = new StringBuilder();
        if (rows.size() < requestedLimit) {
            answer.append("Veritabanında yorumlanmış sadece ")
                    .append(rows.size())
                    .append(" ürün bulundu. ");
        }
        answer.append("En iyi yorumlanmış ")
                .append(requestedLimit)
                .append(" ürün:");
        answer.append("\nYalnızca yorumu olan ürünler dahil edilmiştir.");
        for (Map<String, Object> row : rows) {
            answer.append("\n- ")
                    .append(row.get("product_name"))
                    .append(": ")
                    .append(row.get("average_rating"))
                    .append("/5 ortalama puan, ")
                    .append(row.get("review_count"))
                    .append(" yorum");
        }
        return answer.toString();
    }

    private String buildReviewedProductsDisplaySql(String sql, Long storeId, int requestedLimit) {
        String displaySql = sql;
        if (storeId != null) {
            displaySql = displaySql.replaceFirst("\\?", String.valueOf(storeId));
        }
        return displaySql.replaceFirst("\\?", String.valueOf(requestedLimit));
    }

    private ChatResponse answerLowestRatedProducts(String question, ChatSession session, long startTime) {
        boolean isCorporate = "CORPORATE".equals(session.role());
        if (isCorporate && session.storeId() == null) {
            return new ChatResponse(
                    chatAnalysisService.escapeHtml(question),
                    null,
                    List.of(),
                    "No store is linked to this seller session.",
                    elapsedMs(startTime),
                    "SUCCESS",
                    "ANALYSIS",
                    null,
                    null,
                    buildSessionDetails(session),
                    "Bu hesap için bağlı mağaza bulunamadı.",
                    "NONE",
                    Map.of(),
                    PIPELINE_STEPS
            );
        }

        String sql = isCorporate ? LOWEST_RATED_PRODUCTS_BY_STORE_SQL : LOWEST_RATED_PRODUCTS_SQL;
        List<Map<String, Object>> rows;
        try {
            rows = chatAnalysisService.sanitizeRows(
                    isCorporate
                            ? queryExecutionService.executeQuery(sql, session.storeId())
                            : queryExecutionService.executeQuery(sql)
            );
        } catch (DataAccessException dataAccessException) {
            return new ChatResponse(
                    chatAnalysisService.escapeHtml(question),
                    null,
                    List.of(),
                    "Review analytics query could not be executed.",
                    elapsedMs(startTime),
                    "ERROR",
                    "ANALYSIS",
                    null,
                    null,
                    buildSessionDetails(session),
                    "Şu anda yorum analizi yapılamıyor, lütfen tekrar deneyin.",
                    "NONE",
                    Map.of(),
                    PIPELINE_STEPS
            );
        }
        VisualizationPayload visualization = chatAnalysisService.buildVisualization(rows);

        return new ChatResponse(
                chatAnalysisService.escapeHtml(question),
                chatAnalysisService.escapeHtml(sql),
                rows,
                "Lowest-rated products query executed successfully.",
                elapsedMs(startTime),
                "SUCCESS",
                "ANALYSIS",
                null,
                null,
                buildSessionDetails(session),
                chatAnalysisService.escapeHtml(buildLowestRatedProductsAnswer(rows)),
                visualization.visualizationType(),
                visualization.chartData(),
                PIPELINE_STEPS
        );
    }

    private String buildLowestRatedProductsAnswer(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return "Henüz yorum verisi bulunamadı.";
        }

        StringBuilder answer = new StringBuilder("En kötü yorumlanmış 2 ürün:");
        for (Map<String, Object> row : rows) {
            answer.append("\n- ")
                    .append(row.get("product_name"))
                    .append(": ")
                    .append(row.get("average_rating"))
                    .append("/5 ortalama puan, ")
                    .append(row.get("review_count"))
                    .append(" yorum");
        }
        return answer.toString();
    }

    private String turkishCurrentMonthLabel() {
        YearMonth currentMonth = YearMonth.now();
        String monthName = switch (currentMonth.getMonth()) {
            case JANUARY -> "Ocak";
            case FEBRUARY -> "Şubat";
            case MARCH -> "Mart";
            case APRIL -> "Nisan";
            case MAY -> "Mayıs";
            case JUNE -> "Haziran";
            case JULY -> "Temmuz";
            case AUGUST -> "Ağustos";
            case SEPTEMBER -> "Eylül";
            case OCTOBER -> "Ekim";
            case NOVEMBER -> "Kasım";
            case DECEMBER -> "Aralık";
        };
        return monthName + " " + currentMonth.getYear();
    }

    private String normalizeIntentText(String question) {
        String normalized = question == null ? "" : question.trim().toLowerCase(Locale.ROOT);
        normalized = normalized
                .replace('ı', 'i')
                .replace('ğ', 'g')
                .replace('ü', 'u')
                .replace('ş', 's')
                .replace('ö', 'o')
                .replace('ç', 'c');
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return normalized.replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
    }

    private int extractRequestedLimit(String question, int defaultLimit) {
        String normalized = normalizeIntentText(question);
        for (Pattern pattern : REQUESTED_LIMIT_PATTERNS) {
            Matcher matcher = pattern.matcher(normalized);
            if (matcher.find()) {
                return clampRequestedLimit(Integer.parseInt(matcher.group(1)));
            }
        }

        for (Map.Entry<String, Integer> entry : NUMBER_WORDS.entrySet()) {
            String word = entry.getKey();
            boolean commandNumber =
                    Pattern.compile("\\b(top|first|best|show|list|en iyi|en yuksek)\\s+(?:me\\s+)?" + word + "\\b")
                            .matcher(normalized)
                            .find();
            boolean numberBeforeProduct =
                    Pattern.compile("\\b" + word + "\\s+(?:urun|urunu|urunler|urunleri|products?|items?)\\b")
                            .matcher(normalized)
                            .find();
            if (commandNumber || numberBeforeProduct) {
                return clampRequestedLimit(entry.getValue());
            }
        }

        return clampRequestedLimit(defaultLimit);
    }

    private int clampRequestedLimit(int requestedLimit) {
        return Math.max(1, Math.min(requestedLimit, MAX_REQUESTED_LIMIT));
    }

    private String rateLimitKey(ChatSession session) {
        if (session.email() != null) {
            return session.email();
        }
        return session.clientKey();
    }

    private long elapsedMs(long startTime) {
        return (System.nanoTime() - startTime) / 1_000_000;
    }

}
