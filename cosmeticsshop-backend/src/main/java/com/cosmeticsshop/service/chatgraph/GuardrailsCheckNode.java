package com.cosmeticsshop.service.chatgraph;

import com.cosmeticsshop.dto.GuardrailResult;
import com.cosmeticsshop.service.GuardrailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GuardrailsCheckNode implements ChatGraphNode {

    private static final Logger log = LoggerFactory.getLogger(GuardrailsCheckNode.class);

    private final GuardrailsService guardrailsService;

    public GuardrailsCheckNode(GuardrailsService guardrailsService) {
        this.guardrailsService = guardrailsService;
    }

    @Override
    public String name() {
        return "GuardrailsCheckNode";
    }

    @Override
    public void execute(ChatGraphState state) {
        GuardrailResult result = guardrailsService.inspect(state.getOriginalQuestion(), state.getSession());
        state.setGuardrailResult(result);
        log.info(
                "chat_graph guardrails_check question={} allowed={} category={} reason={} generatedSql={}",
                state.getOriginalQuestion(),
                result.isAllowed(),
                result.getCategory(),
                result.getReason(),
                state.getGeneratedSql()
        );
        if (!result.isAllowed() || "OUT_OF_SCOPE".equals(result.getCategory())) {
            state.setStatus("BLOCKED");
            state.setAgent("GUARDRAIL");
            state.setDetectionType(result.getDetectionType());
            state.setSecurityTitle(result.getCategory());
            state.setErrorMessage(result.getReason());
            state.setFinalResponseMessage(result.getSafeAlternative() == null
                    ? "Bu isteği güvenlik nedeniyle gerçekleştiremiyorum."
                    : result.getSafeAlternative());
            state.setComplete(true);
        }
    }
}
