package com.finguard.common.enums;

/**
 * Risk classification calculated by the Fraud Detection Engine.
 * 0-24: LOW
 * 25-49: MEDIUM
 * 50-74: HIGH
 * 75-100: CRITICAL
 */
public enum RiskLevel {
    LOW(0, 24),
    MEDIUM(25, 49),
    HIGH(50, 74),
    CRITICAL(75, 100);

    private final int minScore;
    private final int maxScore;

    RiskLevel(int minScore, int maxScore) {
        this.minScore = minScore;
        this.maxScore = maxScore;
    }

    public int getMinScore() {
        return minScore;
    }

    public int getMaxScore() {
        return maxScore;
    }

    public static RiskLevel fromScore(int score) {
        if (score >= 75) return CRITICAL;
        if (score >= 50) return HIGH;
        if (score >= 25) return MEDIUM;
        return LOW;
    }
}
