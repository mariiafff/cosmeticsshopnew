package com.cosmeticsshop.dto;

import java.util.List;
import java.util.Map;

public class ChatResponse {

    private String question;
    private String generatedSql;
    private List<Map<String, Object>> rows;
    private String message;
    private long executionTimeMs;
    private String status;
    private String agent;
    private String detectionType;
    private String securityTitle;
    private Map<String, Object> securityDetails;
    private String finalAnswer;
    private String visualizationType;
    private Map<String, Object> chartData;
    private List<String> steps;

    public ChatResponse() {
    }

    public ChatResponse(
            String question,
            String generatedSql,
            List<Map<String, Object>> rows,
            String message,
            long executionTimeMs,
            String status,
            String agent,
            String detectionType,
            String securityTitle,
            Map<String, Object> securityDetails,
            String finalAnswer,
            String visualizationType,
            Map<String, Object> chartData,
            List<String> steps
    ) {
        this.question = question;
        this.generatedSql = generatedSql;
        this.rows = rows;
        this.message = message;
        this.executionTimeMs = executionTimeMs;
        this.status = status;
        this.agent = agent;
        this.detectionType = detectionType;
        this.securityTitle = securityTitle;
        this.securityDetails = securityDetails;
        this.finalAnswer = finalAnswer;
        this.visualizationType = visualizationType;
        this.chartData = chartData;
        this.steps = steps;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getGeneratedSql() {
        return generatedSql;
    }

    public void setGeneratedSql(String generatedSql) {
        this.generatedSql = generatedSql;
    }

    public List<Map<String, Object>> getRows() {
        return rows;
    }

    public void setRows(List<Map<String, Object>> rows) {
        this.rows = rows;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
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
        this.securityDetails = securityDetails;
    }

    public String getFinalAnswer() {
        return finalAnswer;
    }

    public void setFinalAnswer(String finalAnswer) {
        this.finalAnswer = finalAnswer;
    }

    public String getVisualizationType() {
        return visualizationType;
    }

    public void setVisualizationType(String visualizationType) {
        this.visualizationType = visualizationType;
    }

    public Map<String, Object> getChartData() {
        return chartData;
    }

    public void setChartData(Map<String, Object> chartData) {
        this.chartData = chartData;
    }

    public List<String> getSteps() {
        return steps;
    }

    public void setSteps(List<String> steps) {
        this.steps = steps;
    }
}
