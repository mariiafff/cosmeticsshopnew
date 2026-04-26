package com.cosmeticsshop.service;

import com.cosmeticsshop.dto.ChatRequest;
import com.cosmeticsshop.dto.ChatResponse;
import com.cosmeticsshop.exception.GeminiUnavailableException;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class ChatService {

    private static final int MAX_GEMINI_ATTEMPTS = 3;
    private static final long[] RETRY_DELAYS_MS = {1000L, 2000L, 4000L};
    private static final Map<String, String> DEMO_FALLBACK_SQL = Map.of(
            "which membership type spends the most?",
            """
                    select membership_type, avg_spend
                    from ai_safe.membership_summary
                    order by avg_spend desc
                    limit 1
                    """.trim(),
            "which city has the most customers?",
            """
                    select city, total_customers
                    from ai_safe.city_customer_summary
                    order by total_customers desc
                    limit 1
                    """.trim(),
            "show revenue by country",
            """
                    select country, total_revenue
                    from ai_safe.country_revenue_summary
                    order by total_revenue desc
                    """.trim(),
            "show customer segments",
            """
                    select value_segment, membership_type, total_customers
                    from ai_safe.segment_summary
                    order by total_customers desc
                    limit 10
                    """.trim(),
            "top selling products",
            """
                    select product_name, total_units_sold, total_revenue
                    from ai_safe.product_sales_summary
                    order by total_units_sold desc
                    limit 10
                    """.trim()
    );

    private final GeminiSqlService geminiSqlService;
    private final SqlSafetyService sqlSafetyService;
    private final QueryExecutionService queryExecutionService;
    private final Environment environment;

    public ChatService(
            GeminiSqlService geminiSqlService,
            SqlSafetyService sqlSafetyService,
            QueryExecutionService queryExecutionService,
            Environment environment
    ) {
        this.geminiSqlService = geminiSqlService;
        this.sqlSafetyService = sqlSafetyService;
        this.queryExecutionService = queryExecutionService;
        this.environment = environment;
    }

    public ChatResponse ask(ChatRequest request) {
        if (request == null || request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
            throw new IllegalArgumentException("Question is required.");
        }

        long startTime = System.nanoTime();
        String question = request.getQuestion().trim();
        SqlGenerationResult sqlGenerationResult = generateSqlWithRetry(question);
        sqlSafetyService.validate(sqlGenerationResult.sql());

        List<Map<String, Object>> rows = queryExecutionService.executeQuery(sqlGenerationResult.sql());
        long executionTimeMs = (System.nanoTime() - startTime) / 1_000_000;
        return new ChatResponse(
                question,
                sqlGenerationResult.sql(),
                rows,
                sqlGenerationResult.message(),
                executionTimeMs
        );
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

        String normalizedQuestion = normalizeQuestion(question);
        String fallbackSql = DEMO_FALLBACK_SQL.get(normalizedQuestion);
        if (fallbackSql != null) {
            return Optional.of(fallbackSql);
        }

        return Optional.empty();
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

    private record SqlGenerationResult(String sql, String message) {
    }
}
