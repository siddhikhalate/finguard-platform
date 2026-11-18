package com.finguard.ai.fallback;

import com.finguard.ai.dto.AiExplanationRequest;
import com.finguard.ai.dto.AiExplanationResponse;
import com.finguard.common.dto.TriggeredRuleDto;
import com.finguard.common.enums.RiskLevel;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class DeterministicFallbackExplainer {

    public AiExplanationResponse generateFallbackExplanation(AiExplanationRequest request) {
        List<TriggeredRuleDto> rules = request.getTriggeredRules() != null ? request.getTriggeredRules() : List.of();
        List<String> factors = new ArrayList<>();
        List<String> actions = new ArrayList<>();

        StringBuilder summaryBuilder = new StringBuilder();
        summaryBuilder.append(String.format("Transaction of %s %s was classified as %s risk (Score: %d/100). ",
                request.getAmount(), request.getCurrency(), request.getRiskLevel(), request.getRiskScore()));

        if (rules.isEmpty()) {
            summaryBuilder.append("No specific rule details were recorded; risk assessment was assigned directly.");
            factors.add("Overall transaction score exceeds monitoring threshold.");
        } else {
            summaryBuilder.append("Evaluation triggered ").append(rules.size()).append(" compliance rule(s): ");
            for (int i = 0; i < rules.size(); i++) {
                TriggeredRuleDto rule = rules.get(i);
                factors.add(String.format("[%s] %s (+%d pts) - %s",
                        rule.getRuleCode(), rule.getRuleName(), rule.getPointsAdded(), rule.getTriggerReason()));
                summaryBuilder.append(rule.getRuleName());
                if (i < rules.size() - 1) summaryBuilder.append(", ");
            }
            summaryBuilder.append(".");
        }

        // Generate tailored investigation recommendations based on risk level and triggered rules
        if (request.getRiskLevel() == RiskLevel.CRITICAL) {
            actions.add("Immediately execute temporary debit freeze on the cardholder account.");
            actions.add("Initiate out-of-band verification (SMS OTP / Push notification) with cardholder.");
            actions.add("Cross-reference recipient account against internal financial sanctions and anti-money laundering (AML) watchlists.");
        } else if (request.getRiskLevel() == RiskLevel.HIGH) {
            actions.add("Review the past 30 days of transaction velocity and average ticket size for this account.");
            actions.add("Validate the originating IP address and device fingerprint against known proxy/VPN lists.");
            actions.add("Contact the account owner to confirm authorization before settlement.");
        } else {
            actions.add("Log incident for regular post-settlement batch reconciliation.");
            actions.add("Monitor subsequent account transactions for potential velocity escalation.");
        }

        String confidenceStatement = String.format("Deterministic rule-derived assessment. Confidence: HIGH (%d deterministic rules triggered).",
                rules.size());

        return AiExplanationResponse.builder()
                .incidentId(request.getIncidentId())
                .transactionId(request.getTransactionId())
                .summary(summaryBuilder.toString())
                .factors(factors)
                .recommendedActions(actions)
                .confidenceLevel("HIGH")
                .modelUsed("DETERMINISTIC_RULES_FALLBACK_ENGINE")
                .isFallback(true)
                .generatedAt(Instant.now())
                .build();
    }
}
