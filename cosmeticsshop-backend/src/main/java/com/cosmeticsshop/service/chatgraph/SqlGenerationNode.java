package com.cosmeticsshop.service.chatgraph;

import com.cosmeticsshop.service.GeminiSqlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SqlGenerationNode implements ChatGraphNode {

    private static final Logger log = LoggerFactory.getLogger(SqlGenerationNode.class);

    private final GeminiSqlService geminiSqlService;

    public SqlGenerationNode(GeminiSqlService geminiSqlService) {
        this.geminiSqlService = geminiSqlService;
    }

    @Override
    public String name() {
        return "SqlGenerationNode";
    }

    @Override
    public void execute(ChatGraphState state) {
        try {
            boolean strictRetry = state.getRetryCount() > 0;
            state.setSafetyResult(null);
            state.setGeneratedSql(geminiSqlService.generateSql(
                    state.getOriginalQuestion(),
                    state.getSession().role(),
                    state.getSession().storeId(),
                    strictRetry
            ));
            bindScopedParameters(state);
            log.info(
                    "chat_graph generate_sql question={} strictRetry={} role={} userId={} storeId={} generatedSql={} params={}",
                    state.getOriginalQuestion(),
                    strictRetry,
                    state.getSession().role(),
                    state.getSession().userId(),
                    state.getSession().storeId(),
                    state.getGeneratedSql(),
                    state.getSqlParameters()
            );
            state.setResponseMessage(strictRetry
                    ? "Query executed successfully after SQL safety retry."
                    : "Query executed successfully.");
        } catch (RuntimeException ex) {
            log.warn("chat_graph generate_sql failed question={} error={}", state.getOriginalQuestion(), ex.getMessage(), ex);
            state.setStatus("ERROR");
            state.setAgent("ERROR");
            state.setErrorMessage("SQL generation failed.");
            state.setComplete(true);
        }
    }

    private void bindScopedParameters(ChatGraphState state) {
        String sql = state.getGeneratedSql();
        if (sql == null || !sql.contains("?")) {
            state.setSqlParameters(null);
            return;
        }

        int placeholderCount = countPlaceholders(sql);
        if (placeholderCount == 0) {
            state.setSqlParameters(null);
            return;
        }

        if ("CORPORATE".equals(state.getSession().role()) && state.getSession().storeId() != null) {
            state.setSqlParameters(repeatParameter(state.getSession().storeId(), placeholderCount));
            return;
        }

        if ("INDIVIDUAL".equals(state.getSession().role()) && state.getSession().userId() != null) {
            state.setSqlParameters(repeatParameter(state.getSession().userId(), placeholderCount));
            return;
        }

        state.setSqlParameters(null);
    }

    private int countPlaceholders(String sql) {
        int count = 0;
        for (int index = 0; index < sql.length(); index++) {
            if (sql.charAt(index) == '?') {
                count++;
            }
        }
        return count;
    }

    private java.util.List<Object> repeatParameter(Object value, int count) {
        java.util.List<Object> parameters = new java.util.ArrayList<>();
        for (int index = 0; index < count; index++) {
            parameters.add(value);
        }
        return parameters;
    }
}
