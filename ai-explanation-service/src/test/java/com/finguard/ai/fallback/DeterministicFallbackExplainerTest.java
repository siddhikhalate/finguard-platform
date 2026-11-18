package com.finguard.ai.fallback;

import com.finguard.ai.dto.AiExplanationRequest;
import com.finguard.ai.dto.AiExplanationResponse;
import com.finguard.common.dto.TriggeredRuleDto;
import com.finguard.common.enums.RiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DeterministicFallbackExplainerTest {

    private DeterministicFallbackExplainer explainer;

    @BeforeEach
    void setUp() {
        explainer = new DeterministicFallbackExplainer();
    }

    @Test
    @DisplayName("Should generate comprehensive fallback explanation when given triggered rules")
    void shouldGenerateFallbackExplanationWithRules() {
        AiExplanationRequest request = AiExplanationRequest.builder()
                .incidentId(UUID.randomUUID())
                .transactionId(UUID.randomUUID())
                .accountId(UUID.randomUUID())
                .amount(new BigDecimal("15000.00"))
                .currency("USD")
                .location("Lagos, Nigeria")
                .riskScore(70)
                .riskLevel(RiskLevel.HIGH)
                .triggeredRules(List.of(
                        TriggeredRuleDto.builder()
                                .ruleCode("RULE_HIGH_AMOUNT")
                                .ruleName("High Transaction Amount")
                                .pointsAdded(25)
                                .triggerReason("Amount 15000.00 exceeds 10000.00")
                                .build(),
                        TriggeredRuleDto.builder()
                                .ruleCode("RULE_UNUSUAL_LOCATION")
                                .ruleName("Unusual Geographic Location")
                                .pointsAdded(20)
                                .triggerReason("Origin location flagged as anomalous")
                                .build()
                ))
                .build();

        AiExplanationResponse response = explainer.generateFallbackExplanation(request);

        assertNotNull(response);
        assertTrue(response.isFallback());
        assertEquals("DETERMINISTIC_RULES_FALLBACK_ENGINE", response.getModelUsed());
        assertEquals("HIGH", response.getConfidenceLevel());

        // Verify summary contains key data
        assertTrue(response.getSummary().contains("15000.00 USD"));
        assertTrue(response.getSummary().contains("High Transaction Amount"));

        // Verify factors list
        assertEquals(2, response.getFactors().size());
        assertTrue(response.getFactors().get(0).contains("RULE_HIGH_AMOUNT"));

        // Verify recommended actions
        assertFalse(response.getRecommendedActions().isEmpty());
    }

    @Test
    @DisplayName("Should provide urgent freeze action recommendations for CRITICAL risk incidents")
    void shouldProvideCriticalActionRecommendations() {
        AiExplanationRequest request = AiExplanationRequest.builder()
                .incidentId(UUID.randomUUID())
                .transactionId(UUID.randomUUID())
                .accountId(UUID.randomUUID())
                .amount(new BigDecimal("50000.00"))
                .currency("USD")
                .riskScore(85)
                .riskLevel(RiskLevel.CRITICAL)
                .triggeredRules(List.of())
                .build();

        AiExplanationResponse response = explainer.generateFallbackExplanation(request);

        assertNotNull(response);
        assertTrue(response.getRecommendedActions().stream()
                .anyMatch(a -> a.toLowerCase().contains("freeze")));
    }
}
