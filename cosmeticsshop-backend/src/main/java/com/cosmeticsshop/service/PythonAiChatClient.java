package com.cosmeticsshop.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.cosmeticsshop.dto.ChatResponse;
import com.cosmeticsshop.service.ChatAnalysisService.VisualizationPayload;
import com.cosmeticsshop.service.ChatSessionService.ChatSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PythonAiChatClient {

    private static final Logger log = LoggerFactory.getLogger(PythonAiChatClient.class);

    private final RestClient restClient;
    private final ChatAnalysisService chatAnalysisService;
    private final String serviceUrl;
    private final boolean enabled;

    public PythonAiChatClient(
            RestClient restClient,
            ChatAnalysisService chatAnalysisService,
            @Value("${ai.service.url:http://localhost:8000}") String serviceUrl,
            @Value("${ai.service.enabled:true}") boolean enabled
    ) {
        this.restClient = restClient;
        this.chatAnalysisService = chatAnalysisService;
        this.serviceUrl = serviceUrl;
        this.enabled = enabled;
    }

    public Optional<ChatResponse> ask(String question, ChatSession session, long startTime) {
        if (!enabled) {
            return Optional.empty();
        }

        PythonChatRequest request = new PythonChatRequest(
                question,
                session.userId() == null ? null : String.valueOf(session.userId()),
                "ADMIN".equals(session.role()) ? "ADMIN" : "USER"
        );

        try {
            PythonChatResponse response = restClient.post()
                    .uri(serviceUrl + "/chat")
                    .body(request)
                    .retrieve()
                    .body(PythonChatResponse.class);

            if (response == null) {
                log.warn("python_ai service returned an empty response; falling back to Java graph");
                return Optional.empty();
            }
            if (response.error() != null && !response.error().isBlank()) {
                log.warn(
                        "python_ai service returned error; falling back to Java graph question={} error={}",
                        question,
                        response.error()
                );
                return Optional.empty();
            }

            log.info(
                    "python_ai question={} generatedSql={} error={}",
                    question,
                    response.generatedSql(),
                    response.error()
            );
            return Optional.of(toChatResponse(question, session, startTime, response));
        } catch (RestClientException ex) {
            log.warn("python_ai service unavailable; falling back to Java graph: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private ChatResponse toChatResponse(
            String question,
            ChatSession session,
            long startTime,
            PythonChatResponse response
    ) {
        List<Map<String, Object>> rows = response.rows() == null
                ? List.of()
                : chatAnalysisService.sanitizeRows(response.rows());
        boolean hasError = response.error() != null && !response.error().isBlank();
        VisualizationPayload visualization = chatAnalysisService.buildVisualization(rows);

        return new ChatResponse(
                chatAnalysisService.escapeHtml(question),
                hasError ? null : chatAnalysisService.escapeHtml(response.generatedSql()),
                rows,
                response.message(),
                elapsedMs(startTime),
                hasError ? "ERROR" : "SUCCESS",
                "PYTHON_LANGGRAPH",
                hasError ? "Python LangGraph error" : null,
                hasError ? "AI service request failed" : null,
                buildSessionDetails(session),
                chatAnalysisService.escapeHtml(hasError
                        ? response.message()
                        : buildFinalAnswer(question, rows, response.message())),
                visualization.visualizationType(),
                visualization.chartData(),
                List.of(
                        "Ask question",
                        "Analyze question",
                        "Generate SQL",
                        "Validate SQL",
                        "Check guardrails",
                        "Run query",
                        "Show results"
                )
        );
    }

    private String buildFinalAnswer(String question, List<Map<String, Object>> rows, String message) {
        if (rows == null || rows.isEmpty()) {
            return message == null || message.isBlank() ? "No data found for this question." : message;
        }
        return chatAnalysisService.buildFinalAnswer(question, rows);
    }

    private Map<String, Object> buildSessionDetails(ChatSession session) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("role", session.role());
        details.put("engine", "PYTHON_LANGGRAPH");
        details.put("guardrail", "Açık");
        if (session.storeId() != null) {
            details.put("storeId", session.storeId());
        }
        if (session.userId() != null) {
            details.put("userId", session.userId());
        }
        return details;
    }

    private long elapsedMs(long startTime) {
        return (System.nanoTime() - startTime) / 1_000_000;
    }

    private record PythonChatRequest(
            String question,
            @JsonProperty("user_id") String userId,
            String role
    ) {
    }

    private record PythonChatResponse(
            @JsonProperty("generated_sql") String generatedSql,
            List<Map<String, Object>> rows,
            String message,
            String error
    ) {
    }
}
