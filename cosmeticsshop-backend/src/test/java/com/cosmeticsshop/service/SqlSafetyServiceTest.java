package com.cosmeticsshop.service;

import com.cosmeticsshop.util.SqlWhitelist;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SqlSafetyServiceTest {

    private final SqlSafetyService sqlSafetyService = new SqlSafetyService(new SqlWhitelist());

    @Test
    void allowsKnownSafeQuery() {
        assertDoesNotThrow(() -> sqlSafetyService.validate(
                "SELECT membership_type, avg_spend FROM ai_safe.membership_summary ORDER BY avg_spend DESC LIMIT 1"
        ));
    }

    @Test
    void allowsCityCustomerCountSummaryQuery() {
        assertDoesNotThrow(() -> sqlSafetyService.validate(
                "SELECT city, total_customers AS customer_count FROM ai_safe.city_customer_summary ORDER BY total_customers DESC LIMIT 1"
        ));
    }

    @Test
    void allowsCountryRevenueSummaryQuery() {
        assertDoesNotThrow(() -> sqlSafetyService.validate(
                "SELECT country, total_revenue FROM ai_safe.country_revenue_summary ORDER BY total_revenue DESC LIMIT 1"
        ));
    }

    @Test
    void blocksSelectStar() {
        assertThrows(IllegalArgumentException.class, () -> sqlSafetyService.validate(
                "SELECT * FROM ai_safe.membership_summary"
        ));
    }

    @Test
    void blocksUnionExfiltration() {
        assertThrows(IllegalArgumentException.class, () -> sqlSafetyService.validate(
                "SELECT membership_type FROM ai_safe.membership_summary UNION SELECT password_hash FROM public.users"
        ));
    }

    @Test
    void blocksNonWhitelistedColumn() {
        assertThrows(IllegalArgumentException.class, () -> sqlSafetyService.validate(
                "SELECT password_hash FROM ai_safe.customer_profiles LIMIT 1"
        ));
    }

    @Test
    void blocksLimitOverHundred() {
        assertThrows(IllegalArgumentException.class, () -> sqlSafetyService.validate(
                "SELECT city, total_customers FROM ai_safe.city_customer_summary ORDER BY total_customers DESC LIMIT 500"
        ));
    }
}
