package com.cosmeticsshop.service.chatgraph;

import com.cosmeticsshop.dto.ChatResponse;
import com.cosmeticsshop.service.ChatSessionService.ChatSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ChatGraphOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(ChatGraphOrchestrator.class);

    private final QuestionAnalysisNode questionAnalysisNode;
    private final SqlGenerationNode sqlGenerationNode;
    private final SqlSafetyCheckNode sqlSafetyCheckNode;
    private final GuardrailsCheckNode guardrailsCheckNode;
    private final QueryExecutionNode queryExecutionNode;
    private final ResponseFormattingNode responseFormattingNode;
    private final ErrorHandlingNode errorHandlingNode;

    public ChatGraphOrchestrator(
            QuestionAnalysisNode questionAnalysisNode,
            SqlGenerationNode sqlGenerationNode,
            SqlSafetyCheckNode sqlSafetyCheckNode,
            GuardrailsCheckNode guardrailsCheckNode,
            QueryExecutionNode queryExecutionNode,
            ResponseFormattingNode responseFormattingNode,
            ErrorHandlingNode errorHandlingNode
    ) {
        this.questionAnalysisNode = questionAnalysisNode;
        this.sqlGenerationNode = sqlGenerationNode;
        this.sqlSafetyCheckNode = sqlSafetyCheckNode;
        this.guardrailsCheckNode = guardrailsCheckNode;
        this.queryExecutionNode = queryExecutionNode;
        this.responseFormattingNode = responseFormattingNode;
        this.errorHandlingNode = errorHandlingNode;
    }

    public ChatResponse run(String question, ChatSession session, long startTimeNanos) {
        ChatGraphState state = new ChatGraphState(question, session, startTimeNanos);
        log.info(
                "chat_graph start question={} role={} userId={} storeId={}",
                question,
                session.role(),
                session.userId(),
                session.storeId()
        );

        runNode(questionAnalysisNode, state);
        if (state.isComplete()) {
            return finalizeWithError(state);
        }

        runNode(sqlGenerationNode, state);
        if (state.isComplete()) {
            return finalizeWithError(state);
        }

        runNode(sqlSafetyCheckNode, state);
        if (state.getSafetyResult() != null && !state.getSafetyResult().safe()) {
            logNodeResult(sqlSafetyCheckNode.name(), false, state, state.getSafetyResult().reason());
            if (state.getRetryCount() == 0) {
                state.incrementRetryCount();
                runNode(sqlGenerationNode, state);
                if (state.isComplete()) {
                    return finalizeWithError(state);
                }
                runNode(sqlSafetyCheckNode, state);
            }
            if (state.getSafetyResult() != null && !state.getSafetyResult().safe()) {
                state.setStatus("ERROR");
                state.setAgent("VALIDATOR");
                state.setErrorMessage("SQL validation failure");
                state.setResponseMessage("Generated SQL did not pass validation.");
                state.setFinalResponseMessage("The request was stopped because the generated SQL violated safety rules.");
                return finalizeWithError(state);
            }
        }

        runNode(guardrailsCheckNode, state);
        if (state.isComplete()) {
            return finalizeWithError(state);
        }

        runNode(queryExecutionNode, state);
        if (state.isComplete()) {
            return finalizeWithError(state);
        }

        runNode(responseFormattingNode, state);
        return state.getResponse();
    }

    private ChatResponse finalizeWithError(ChatGraphState state) {
        runNode(errorHandlingNode, state);
        runNode(responseFormattingNode, state);
        return state.getResponse();
    }

    private void runNode(ChatGraphNode node, ChatGraphState state) {
        long nodeStart = System.nanoTime();
        try {
            node.execute(state);
            logNodeResult(node.name(), isSuccess(state), state, rejectionReason(state), nodeStart);
        } catch (RuntimeException ex) {
            state.setStatus("ERROR");
            state.setAgent("ERROR");
            state.setErrorMessage("Unexpected graph node failure.");
            state.setFinalResponseMessage("Analytics query failed due to internal issue.");
            log.warn(
                    "chat_graph node={} failed question={} role={} userId={} storeId={} generatedSql={} params={} errorClass={} error={}",
                    node.name(),
                    state.getOriginalQuestion(),
                    state.getSession().role(),
                    state.getSession().userId(),
                    state.getSession().storeId(),
                    state.getGeneratedSql(),
                    state.getSqlParameters(),
                    ex.getClass().getSimpleName(),
                    ex.getMessage(),
                    ex
            );
            logNodeResult(node.name(), false, state, ex.getClass().getSimpleName(), nodeStart);
        }
    }

    private boolean isSuccess(ChatGraphState state) {
        if ("BLOCKED".equals(state.getStatus()) || "ERROR".equals(state.getStatus())) {
            return false;
        }
        return state.getSafetyResult() == null || state.getSafetyResult().safe();
    }

    private String rejectionReason(ChatGraphState state) {
        if (state.getSafetyResult() != null && !state.getSafetyResult().safe()) {
            return state.getSafetyResult().reason();
        }
        return state.getErrorMessage();
    }

    private void logNodeResult(String nodeName, boolean success, ChatGraphState state, String reason) {
        logNodeResult(nodeName, success, state, reason, System.nanoTime());
    }

    private void logNodeResult(String nodeName, boolean success, ChatGraphState state, String reason, long nodeStart) {
        long elapsedMs = (System.nanoTime() - nodeStart) / 1_000_000;
        String safeSql = state.getSafetyResult() != null && state.getSafetyResult().safe()
                ? state.getGeneratedSql()
                : null;
        if (success) {
            log.info(
                    "chat_graph node={} success=true question={} role={} userId={} storeId={} generatedSql={} params={} safety={} guardrail={} elapsedMs={}",
                    nodeName,
                    state.getOriginalQuestion(),
                    state.getSession().role(),
                    state.getSession().userId(),
                    state.getSession().storeId(),
                    safeSql,
                    state.getSqlParameters(),
                    state.getSafetyResult(),
                    state.getGuardrailResult(),
                    elapsedMs
            );
        } else {
            log.warn(
                    "chat_graph node={} success=false question={} role={} userId={} storeId={} generatedSql={} params={} safety={} guardrail={} reason={} elapsedMs={}",
                    nodeName,
                    state.getOriginalQuestion(),
                    state.getSession().role(),
                    state.getSession().userId(),
                    state.getSession().storeId(),
                    state.getGeneratedSql(),
                    state.getSqlParameters(),
                    state.getSafetyResult(),
                    state.getGuardrailResult(),
                    reason,
                    elapsedMs
            );
        }
    }
}
