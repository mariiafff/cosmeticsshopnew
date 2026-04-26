package com.cosmeticsshop.service;

import com.cosmeticsshop.dto.ChatRequest;
import com.cosmeticsshop.dto.ChatResponse;
import com.cosmeticsshop.dto.GuardrailResult;
import com.cosmeticsshop.exception.GeminiUnavailableException;
import com.cosmeticsshop.service.ChatAnalysisService.VisualizationPayload;
import com.cosmeticsshop.service.ChatSessionService.ChatSession;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class ChatService {

    private static final int MAX_GEMINI_ATTEMPTS = 3;
    private static final long[] RETRY_DELAYS_MS = {1000L, 2000L, 4000L};
    private static final List<String> PIPELINE_STEPS = List.of(
            "Ask question",
            "Generate SQL",
            "Validate SQL",
            "Run query",
            "Show results"
    );

    private static final Map<String, String> DEMO_FALLBACK_SQL = Map.of(
            "which membership type spends the most?",
            "select membership_type, avg_spend from ai_safe.membership_summary order by avg_spend desc limit 1",
            "which city has the most customers?",
            "select city, total_customers from ai_safe.city_customer_summary order by total_customers desc limit 1",
            "top selling products",
            "select product_name, total_revenue from ai_safe.product_sales_summary order by total_revenue desc limit 5",
            "which country generates the most revenue?",
            "select country, total_revenue from ai_safe.country_revenue_summary order by total_revenue desc limit 1",
            "show revenue by country",
            "select country, total_revenue from ai_safe.country_revenue_summary order by total_revenue desc limit 5"
    );

    private final GeminiSqlService geminiSqlService;
    private final SqlSafetyService sqlSafetyService;
    private final QueryExecutionService queryExecutionService;
    private final Environment environment;
    private final GuardrailsService guardrailsService;
    private final ChatSessionService chatSessionService;
    private final ChatRateLimitService chatRateLimitService;
    private final ChatAnalysisService chatAnalysisService;

    public ChatService(
            GeminiSqlService geminiSqlService,
            SqlSafetyService sqlSafetyService,
            QueryExecutionService queryExecutionService,
            Environment environment,
            GuardrailsService guardrailsService,
            ChatSessionService chatSessionService,
            ChatRateLimitService chatRateLimitService,
            ChatAnalysisService chatAnalysisService
    ) {
        this.geminiSqlService = geminiSqlService;
        this.sqlSafetyService = sqlSafetyService;
        this.queryExecutionService = queryExecutionService;
        this.environment = environment;
        this.guardrailsService = guardrailsService;
        this.chatSessionService = chatSessionService;
        this.chatRateLimitService = chatRateLimitService;
        this.chatAnalysisService = chatAnalysisService;
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
        if (!guardrailResult.isAllowed()) {
            return buildBlockedResponse(question, guardrailResult.getReason(), guardrailResult, session, startTime);
        }

        try {
            SqlGenerationResult sqlGenerationResult = generateSqlWithRetry(question);
            sqlSafetyService.validate(sqlGenerationResult.sql());

            List<Map<String, Object>> rows = chatAnalysisService.sanitizeRows(
                    queryExecutionService.executeQuery(sqlGenerationResult.sql())
            );
            VisualizationPayload visualization = chatAnalysisService.buildVisualization(rows);
            long executionTimeMs = elapsedMs(startTime);

            return new ChatResponse(
                    chatAnalysisService.escapeHtml(question),
                    chatAnalysisService.escapeHtml(sqlGenerationResult.sql()),
                    rows,
                    sqlGenerationResult.message(),
                    executionTimeMs,
                    "SUCCESS",
                    "ANALYSIS",
                    null,
                    null,
                    buildSessionDetails(session),
                    chatAnalysisService.escapeHtml(chatAnalysisService.buildFinalAnswer(question, rows)),
                    visualization.visualizationType(),
                    visualization.chartData(),
                    PIPELINE_STEPS
            );
        } catch (GeminiUnavailableException geminiUnavailableException) {
            return new ChatResponse(
                    chatAnalysisService.escapeHtml(question),
                    null,
                    List.of(),
                    "Gemini is temporarily unavailable.",
                    elapsedMs(startTime),
                    "ERROR",
                    "ERROR",
                    "Upstream model unavailable",
                    "Model geçici olarak kullanılamıyor",
                    buildSessionDetails(session),
                    "The assistant could not reach the SQL model right now. Please try again shortly.",
                    "NONE",
                    Map.of(),
                    PIPELINE_STEPS
            );
        } catch (IllegalArgumentException validationException) {
            return new ChatResponse(
                    chatAnalysisService.escapeHtml(question),
                    null,
                    List.of(),
                    "Generated SQL did not pass validation.",
                    elapsedMs(startTime),
                    "ERROR",
                    "VALIDATOR",
                    "SQL validation failure",
                    "SQL güvenlik doğrulaması başarısız oldu",
                    Map.of(
                            "reason", chatAnalysisService.escapeHtml(validationException.getMessage()),
                            "role", session.role()
                    ),
                    "The request was stopped because the generated SQL violated safety rules.",
                    "NONE",
                    Map.of(),
                    PIPELINE_STEPS
            );
        }
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

    private SqlGenerationResult generateSqlWithRetry(String question) {
        RuntimeException lastTransientError = null;

        for (int attempt = 1; attempt <= MAX_GEMINI_ATTEMPTS; attempt++) {
            try {
                return new SqlGenerationResult(
                        geminiSqlService.generateSql(question),
                        "Query executed successfully."
                );
            } catch (RuntimeException ex) {
                if (!isRetryableGeminiError(ex)) {
                    throw ex;
                }

                lastTransientError = ex;
                if (attempt == MAX_GEMINI_ATTEMPTS) {
                    break;
                }

                sleepBeforeRetry(attempt);
            }
        }

        Optional<String> fallbackSql = buildDevelopmentFallbackSql(question, lastTransientError);
        if (fallbackSql.isPresent()) {
            return new SqlGenerationResult(
                    fallbackSql.get(),
                    "Gemini unavailable, fallback query executed."
            );
        }

        throw new GeminiUnavailableException(
                "Gemini is temporarily unavailable. Please try again in a moment.",
                lastTransientError
        );
    }

    private boolean isRetryableGeminiError(RuntimeException ex) {
        if (ex instanceof ResourceAccessException) {
            return true;
        }

        if (ex instanceof RestClientResponseException responseException) {
            int statusCode = responseException.getStatusCode().value();
            return statusCode == 429 || statusCode == 503;
        }

        Throwable cause = ex.getCause();
        while (cause != null) {
            if (cause instanceof java.net.SocketTimeoutException || cause instanceof java.net.http.HttpTimeoutException) {
                return true;
            }
            cause = cause.getCause();
        }

        return false;
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(RETRY_DELAYS_MS[attempt - 1]);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new GeminiUnavailableException(
                    "Gemini request was interrupted. Please try again.",
                    interruptedException
            );
        }
    }

    private Optional<String> buildDevelopmentFallbackSql(String question, RuntimeException lastTransientError) {
        if (!isDevelopmentEnvironment() || lastTransientError == null) {
            return Optional.empty();
        }

        String fallbackSql = DEMO_FALLBACK_SQL.get(normalizeQuestion(question));
        return Optional.ofNullable(fallbackSql);
    }

    private String normalizeQuestion(String question) {
        return question == null ? "" : question.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isDevelopmentEnvironment() {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length == 0) {
            return true;
        }

        for (String profile : activeProfiles) {
            if ("dev".equalsIgnoreCase(profile) || "development".equalsIgnoreCase(profile) || "local".equalsIgnoreCase(profile)) {
                return true;
            }
        }

        return false;
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

    private record SqlGenerationResult(String sql, String message) {
    }
}
