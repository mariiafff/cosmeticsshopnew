package com.cosmeticsshop.dto;

import java.util.List;
import java.util.Map;

public class ChatResponse {

    private final String question;
    private final boolean inScope;
    private final String sqlQuery;
    private final String answer;
    private final String visualizationHint;
    private final List<Map<String, Object>> rows;

    public ChatResponse(
            String question,
            boolean inScope,
            String sqlQuery,
            String answer,
            String visualizationHint,
            List<Map<String, Object>> rows
    ) {
        this.question = question;
        this.inScope = inScope;
        this.sqlQuery = sqlQuery;
        this.answer = answer;
        this.visualizationHint = visualizationHint;
        this.rows = rows;
    }

    public String getQuestion() {
        return question;
    }

    public boolean isInScope() {
        return inScope;
    }

    public String getSqlQuery() {
        return sqlQuery;
    }

    public String getAnswer() {
        return answer;
    }

    public String getVisualizationHint() {
        return visualizationHint;
    }

    public List<Map<String, Object>> getRows() {
        return rows;
    }
}
