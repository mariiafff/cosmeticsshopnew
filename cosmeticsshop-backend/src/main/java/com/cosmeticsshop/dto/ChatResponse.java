package com.cosmeticsshop.dto;

import java.util.List;
import java.util.Map;

public class ChatResponse {

    private String question;
    private String generatedSql;
    private List<Map<String, Object>> rows;
    private String message;
    private long executionTimeMs;

    public ChatResponse() {
    }

    public ChatResponse(
            String question,
            String generatedSql,
            List<Map<String, Object>> rows,
            String message,
            long executionTimeMs
    ) {
        this.question = question;
        this.generatedSql = generatedSql;
        this.rows = rows;
        this.message = message;
        this.executionTimeMs = executionTimeMs;
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
}
