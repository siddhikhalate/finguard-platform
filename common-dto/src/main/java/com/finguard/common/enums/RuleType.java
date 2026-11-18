package com.finguard.common.enums;

/**
 * Categorization of deterministic fraud evaluation rules.
 */
public enum RuleType {
    AMOUNT_THRESHOLD,
    VELOCITY_BURST,
    LOCATION_DEVIATION,
    RAPID_ACTIVITY,
    FAILED_ATTEMPTS
}
