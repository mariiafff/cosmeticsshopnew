package com.cosmeticsshop.service.chatgraph;

import com.cosmeticsshop.dto.ChatResponse;
import com.cosmeticsshop.service.ChatAnalysisService;
import com.cosmeticsshop.service.ChatAnalysisService.VisualizationPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ResponseFormattingNode implements ChatGraphNode {

    private static final Logger log = LoggerFactory.getLogger(ResponseFormattingNode.class);

    static final List<String> GRAPH_STEPS = List.of(
            "Ask question",
            "Generate SQL",
            "Validate SQL",
            "Check guardrails",
            "Run query",
            "Show results"
    );

    private final ChatAnalysisService chatAnalysisService;

    public ResponseFormattingNode(ChatAnalysisService chatAnalysisService) {
        this.chatAnalysisService = chatAnalysisService;
    }

    @Override
    public String name() {
        return "ResponseFormattingNode";
    }

    @Override
    public void execute(ChatGraphState state) {
        if (state.getFinalResponseMessage() == null) {
            state.setFinalResponseMessage(buildFinalMessage(state));
        }
        log.info(
                "chat_graph format_response question={} status={} generatedSql={} rowCount={} finalMessage={}",
                state.getOriginalQuestion(),
                state.getStatus(),
                shouldExposeSql(state) ? state.getGeneratedSql() : null,
                state.getExecutionRows().size(),
                state.getFinalResponseMessage()
        );

        VisualizationPayload visualization = chatAnalysisService.buildVisualization(state.getExecutionRows());
        state.setVisualization(visualization);
        state.setExecutionTimeMs(elapsedMs(state));

        state.setResponse(new ChatResponse(
                chatAnalysisService.escapeHtml(state.getOriginalQuestion()),
                shouldExposeSql(state) ? chatAnalysisService.escapeHtml(state.getGeneratedSql()) : null,
                state.getExecutionRows(),
                state.getResponseMessage(),
                state.getExecutionTimeMs(),
                state.getStatus(),
                state.getAgent(),
                state.getDetectionType(),
                state.getSecurityTitle(),
                state.getSecurityDetails().isEmpty() ? buildSessionDetails(state) : state.getSecurityDetails(),
                chatAnalysisService.escapeHtml(state.getFinalResponseMessage()),
                visualization.visualizationType(),
                visualization.chartData(),
                GRAPH_STEPS
        ));
        state.setComplete(true);
    }

    private String buildFinalMessage(ChatGraphState state) {
        if (state.getExecutionRows().isEmpty()) {
            return "No data found for this question. Try narrowing the time range or asking for a broader aggregate.";
        }
        return chatAnalysisService.buildFinalAnswer(state.getOriginalQuestion(), state.getExecutionRows());
    }

    private boolean shouldExposeSql(ChatGraphState state) {
        return "SUCCESS".equals(state.getStatus())
                && state.getSafetyResult() != null
                && state.getSafetyResult().safe();
    }

    private Map<String, Object> buildSessionDetails(ChatGraphState state) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("role", state.getSession().role());
        details.put("guardrail", "Açık");
        if (state.getSession().storeId() != null) {
            details.put("storeId", state.getSession().storeId());
        }
        if (state.getSession().userId() != null) {
            details.put("userId", state.getSession().userId());
        }
        return details;
    }

    private long elapsedMs(ChatGraphState state) {
        return (System.nanoTime() - state.getStartTimeNanos()) / 1_000_000;
    }
}
