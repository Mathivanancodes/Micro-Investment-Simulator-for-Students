package com.microinvestment.simulator.entity;

import com.microinvestment.simulator.exception.BusinessRuleException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Shared storage invariants; request validation is still performed at the API boundary. */
final class EntityRules {
    private EntityRules() { }

    static BigDecimal decimal(BigDecimal value, int scale, boolean allowZero, String field) {
        if (value == null || value.signum() < 0 || (!allowZero && value.signum() == 0)) {
            throw new BusinessRuleException(field + " must be " + (allowZero ? "nonnegative" : "positive"));
        }
        try {
            BigDecimal normalized = value.setScale(scale, RoundingMode.UNNECESSARY);
            if (normalized.precision() > 15) {
                throw new BusinessRuleException(field + " is too large");
            }
            return normalized;
        } catch (ArithmeticException exception) {
            throw new BusinessRuleException(field + " supports at most " + scale + " decimal places");
        }
    }

    static String text(String value, int max, String field) {
        if (value == null || value.isBlank() || value.strip().length() > max) {
            throw new BusinessRuleException(field + " must contain 1 to " + max + " characters");
        }
        return value.strip();
    }
}
