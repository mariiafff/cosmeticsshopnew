package com.cosmeticsshop.dto;

public class GuardrailResult {

    private boolean allowed;
    private String category;
    private String severity;
    private String reason;
    private String detectionType;
    private String blockedAction;
    private String safeAlternative;

    public GuardrailResult() {
    }

    public GuardrailResult(
            boolean allowed,
            String category,
            String severity,
            String reason,
            String detectionType,
            String blockedAction,
            String safeAlternative
    ) {
        this.allowed = allowed;
        this.category = category;
        this.severity = severity;
        this.reason = reason;
        this.detectionType = detectionType;
        this.blockedAction = blockedAction;
        this.safeAlternative = safeAlternative;
    }

    public static GuardrailResult allow() {
        return new GuardrailResult(true, "ALLOW", "INFO", null, null, null, null);
    }

    public static GuardrailResult block(
            String category,
            String severity,
            String reason,
            String detectionType,
            String blockedAction,
            String safeAlternative
    ) {
        return new GuardrailResult(false, category, severity, reason, detectionType, blockedAction, safeAlternative);
    }

    public boolean isAllowed() {
        return allowed;
    }

    public String getCategory() {
        return category;
    }

    public String getSeverity() {
        return severity;
    }

    public String getReason() {
        return reason;
    }

    public String getDetectionType() {
        return detectionType;
    }

    public String getBlockedAction() {
        return blockedAction;
    }

    public String getSafeAlternative() {
        return safeAlternative;
    }
}
