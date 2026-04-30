package com.cosmeticsshop.service.chatgraph;

import com.cosmeticsshop.service.ChatAnalysisService;
import com.cosmeticsshop.service.QueryExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

@Component
public class QueryExecutionNode implements ChatGraphNode {

    private static final Logger log = LoggerFactory.getLogger(QueryExecutionNode.class);

    private final QueryExecutionService queryExecutionService;
    private final ChatAnalysisService chatAnalysisService;

    public QueryExecutionNode(QueryExecutionService queryExecutionService, ChatAnalysisService chatAnalysisService) {
        this.queryExecutionService = queryExecutionService;
        this.chatAnalysisService = chatAnalysisService;
    }

    @Override
    public String name() {
        return "QueryExecutionNode";
    }

    @Override
    public void execute(ChatGraphState state) {
        try {
            state.setExecutionRows(chatAnalysisService.sanitizeRows(
                    queryExecutionService.executeQuery(
                            state.getGeneratedSql(),
                            state.getSqlParameters().toArray()
                    )
            ));
            log.info(
                    "chat_graph execute_query question={} role={} userId={} storeId={} generatedSql={} params={} rowCount={}",
                    state.getOriginalQuestion(),
                    state.getSession().role(),
                    state.getSession().userId(),
                    state.getSession().storeId(),
                    state.getGeneratedSql(),
                    state.getSqlParameters(),
                    state.getExecutionRows().size()
            );
        } catch (DataAccessException ex) {
            log.warn(
                    "chat_graph execute_query failed question={} role={} userId={} storeId={} generatedSql={} params={} dbError={}",
                    state.getOriginalQuestion(),
                    state.getSession().role(),
                    state.getSession().userId(),
                    state.getSession().storeId(),
                    state.getGeneratedSql(),
                    state.getSqlParameters(),
                    ex.getMessage(),
                    ex
            );
            state.setStatus("ERROR");
            state.setAgent("ERROR");
            state.setErrorMessage("Database execution failed.");
            if (isUserPercentageQuery(state)) {
                state.setResponseMessage("User percentage analytics query failed.");
                state.setFinalResponseMessage("I can show your last order and total spending, but percentage breakdown is not supported yet.");
            } else {
                state.setResponseMessage("Analytics query failed due to internal issue.");
                state.setFinalResponseMessage("Analytics query failed due to internal issue.");
            }
            state.setComplete(true);
        }
    }

    private boolean isUserPercentageQuery(ChatGraphState state) {
        String sql = state.getGeneratedSql() == null ? "" : state.getGeneratedSql().toLowerCase(java.util.Locale.ROOT);
        String question = state.getOriginalQuestion() == null ? "" : state.getOriginalQuestion().toLowerCase(java.util.Locale.ROOT);
        return "INDIVIDUAL".equals(state.getSession().role())
                && sql.contains("ai_safe.user_")
                && (sql.contains("percentage_of_total_spent")
                || question.contains("percentage")
                || question.contains("percent")
                || question.contains("yüzde")
                || question.contains("yuzde")
                || question.contains("oran"));
    }
}
