package com.cosmeticsshop.service.chatgraph;

import com.cosmeticsshop.dto.ChatResponse;
import com.cosmeticsshop.dto.GuardrailResult;
import com.cosmeticsshop.service.ChatAnalysisService.VisualizationPayload;
import com.cosmeticsshop.service.ChatSessionService.ChatSession;

import java.util.List;
import java.util.Map;

public class ChatGraphState {

    private final String originalQuestion;
    private final ChatSession session;
    private final long startTimeNanos;
    private String generatedSql;
    private List<Object> sqlParameters = List.of();
    private SafetyResult safetyResult;
    private GuardrailResult guardrailResult;
    private List<Map<String, Object>> executionRows = List.of();
    private String errorMessage;
    private String finalResponseMessage;
    private int retryCount;
    private long executionTimeMs;
    private boolean complete;
    private String status = "SUCCESS";
    private String agent = "ANALYSIS";
    private String detectionType;
    private String securityTitle;
    private Map<String, Object> securityDetails = Map.of();
    private String responseMessage = "Query executed successfully.";
    private VisualizationPayload visualization = new VisualizationPayload("NONE", Map.of());
    private ChatResponse response;

    public ChatGraphState(String originalQuestion, ChatSession session, long startTimeNanos) {
        this.originalQuestion = originalQuestion;
        this.session = session;
        this.startTimeNanos = startTimeNanos;
    }

    public String getOriginalQuestion() {
        return originalQuestion;
    }

    public ChatSession getSession() {
        return session;
    }

    public long getStartTimeNanos() {
        return startTimeNanos;
    }

    public String getGeneratedSql() {
        return generatedSql;
    }

    public void setGeneratedSql(String generatedSql) {
        this.generatedSql = generatedSql;
    }

    public List<Object> getSqlParameters() {
        return sqlParameters;
    }

    public void setSqlParameters(List<Object> sqlParameters) {
        this.sqlParameters = sqlParameters == null ? List.of() : sqlParameters;
    }

    public SafetyResult getSafetyResult() {
        return safetyResult;
    }

    public void setSafetyResult(SafetyResult safetyResult) {
        this.safetyResult = safetyResult;
    }

    public GuardrailResult getGuardrailResult() {
        return guardrailResult;
    }

    public void setGuardrailResult(GuardrailResult guardrailResult) {
        this.guardrailResult = guardrailResult;
    }

    public List<Map<String, Object>> getExecutionRows() {
        return executionRows;
    }

    public void setExecutionRows(List<Map<String, Object>> executionRows) {
        this.executionRows = executionRows == null ? List.of() : executionRows;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getFinalResponseMessage() {
        return finalResponseMessage;
    }

    public void setFinalResponseMessage(String finalResponseMessage) {
        this.finalResponseMessage = finalResponseMessage;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void incrementRetryCount() {
        retryCount++;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAgent() {
        return agent;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }

    public String getDetectionType() {
        return detectionType;
    }

    public void setDetectionType(String detectionType) {
        this.detectionType = detectionType;
    }

    public String getSecurityTitle() {
        return securityTitle;
    }

    public void setSecurityTitle(String securityTitle) {
        this.securityTitle = securityTitle;
    }

    public Map<String, Object> getSecurityDetails() {
        return securityDetails;
    }

    public void setSecurityDetails(Map<String, Object> securityDetails) {
        this.securityDetails = securityDetails == null ? Map.of() : securityDetails;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }

    public VisualizationPayload getVisualization() {
        return visualization;
    }

    public void setVisualization(VisualizationPayload visualization) {
        this.visualization = visualization == null ? new VisualizationPayload("NONE", Map.of()) : visualization;
    }

    public ChatResponse getResponse() {
        return response;
    }

    public void setResponse(ChatResponse response) {
        this.response = response;
    }

    public record SafetyResult(boolean safe, String reason) {

        public static SafetyResult allowed() {
            return new SafetyResult(true, null);
        }

        public static SafetyResult blocked(String reason) {
            return new SafetyResult(false, reason);
        }
    }
}
