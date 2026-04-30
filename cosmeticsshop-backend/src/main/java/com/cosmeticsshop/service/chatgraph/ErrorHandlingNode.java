package com.cosmeticsshop.service.chatgraph;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ErrorHandlingNode implements ChatGraphNode {

    @Override
    public String name() {
        return "ErrorHandlingNode";
    }

    @Override
    public void execute(ChatGraphState state) {
        if ("BLOCKED".equals(state.getStatus())) {
            Map<String, Object> securityDetails = new LinkedHashMap<>();
            securityDetails.put("role", state.getSession().role());
            if (state.getSession().storeId() != null) {
                securityDetails.put("sessionStoreId", state.getSession().storeId());
            }
            if (state.getSession().userId() != null) {
                securityDetails.put("sessionUserId", state.getSession().userId());
            }
            if (state.getGuardrailResult() != null) {
                securityDetails.put("severity", state.getGuardrailResult().getSeverity());
                securityDetails.put("reason", state.getGuardrailResult().getReason());
                securityDetails.put("detectionType", state.getGuardrailResult().getDetectionType());
                securityDetails.put("blockedAction", state.getGuardrailResult().getBlockedAction());
                if (state.getGuardrailResult().getSafeAlternative() != null) {
                    securityDetails.put("safeAlternative", state.getGuardrailResult().getSafeAlternative());
                }
            }
            state.setSecurityDetails(securityDetails);
            state.setResponseMessage(state.getErrorMessage() == null
                    ? "This request was blocked by security guardrails."
                    : state.getErrorMessage());
            return;
        }

        state.setStatus("ERROR");
        state.setAgent(state.getAgent() == null ? "ERROR" : state.getAgent());
        if (state.getFinalResponseMessage() == null) {
            state.setFinalResponseMessage("Analytics query failed due to internal issue.");
        }
        if (state.getResponseMessage() == null || "Query executed successfully.".equals(state.getResponseMessage())) {
            state.setResponseMessage("The request could not be completed safely.");
        }
    }
}
