package com.cosmeticsshop.service.chatgraph;

import com.cosmeticsshop.service.SqlSafetyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SqlSafetyCheckNode implements ChatGraphNode {

    private static final Logger log = LoggerFactory.getLogger(SqlSafetyCheckNode.class);

    private final SqlSafetyService sqlSafetyService;

    public SqlSafetyCheckNode(SqlSafetyService sqlSafetyService) {
        this.sqlSafetyService = sqlSafetyService;
    }

    @Override
    public String name() {
        return "SqlSafetyCheckNode";
    }

    @Override
    public void execute(ChatGraphState state) {
        try {
            sqlSafetyService.validate(state.getGeneratedSql());
            String scopeReason = validateRoleScope(state);
            if (scopeReason != null) {
                state.setSafetyResult(ChatGraphState.SafetyResult.blocked(scopeReason));
                log.warn(
                        "chat_graph safety_check question={} safe=false reason={} generatedSql={}",
                        state.getOriginalQuestion(),
                        scopeReason,
                        state.getGeneratedSql()
                );
                return;
            }
            state.setSafetyResult(ChatGraphState.SafetyResult.allowed());
            log.info(
                    "chat_graph safety_check question={} safe=true generatedSql={}",
                    state.getOriginalQuestion(),
                    state.getGeneratedSql()
            );
        } catch (IllegalArgumentException ex) {
            state.setSafetyResult(ChatGraphState.SafetyResult.blocked(ex.getMessage()));
            log.warn(
                    "chat_graph safety_check question={} safe=false reason={} generatedSql={}",
                    state.getOriginalQuestion(),
                    ex.getMessage(),
                    state.getGeneratedSql()
            );
        }
    }

    private String validateRoleScope(ChatGraphState state) {
        String sql = state.getGeneratedSql() == null ? "" : state.getGeneratedSql().toLowerCase(java.util.Locale.ROOT);
        if ("CORPORATE".equals(state.getSession().role())) {
            if (sql.contains("ai_safe.seller_")
                    && !sql.contains("store_id = ?")
                    && !sql.contains("seller_user_id = ?")) {
                return "Seller analytics queries must be scoped with a bound seller/store parameter.";
            }
            if (sql.matches(".*\\bstore_id\\s*=\\s*\\d+.*") || sql.matches(".*\\bseller_user_id\\s*=\\s*\\d+.*")) {
                return "Seller analytics queries must use parameter binding for seller/store scope.";
            }
        }
        if ("INDIVIDUAL".equals(state.getSession().role())) {
            if (sql.contains("ai_safe.user_") && !sql.contains("customer_id = ?") && !sql.contains("customer_id=?")) {
                return "User analytics queries must be scoped with a bound customer parameter.";
            }
            if (sql.matches(".*\\bcustomer_id\\s*=\\s*\\d+.*") || sql.matches(".*\\buser_id\\s*=\\s*\\d+.*")) {
                return "User analytics queries must use parameter binding for customer scope.";
            }
        }
        if (sql.contains("ai_safe.user_")
                && !sql.contains("ai_safe.user_order_items") // Allow category analysis for sellers
                && !"INDIVIDUAL".equals(state.getSession().role())
                && !"ADMIN".equals(state.getSession().role())) {
            return "User analytics queries are only available to individual users or admins.";
        }
        return null;
    }
}
