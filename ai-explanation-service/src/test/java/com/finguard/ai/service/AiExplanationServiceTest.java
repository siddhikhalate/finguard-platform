package com.finguard.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finguard.ai.dto.AiExplanationRequest;
import com.finguard.ai.dto.AiExplanationResponse;
import com.finguard.ai.fallback.DeterministicFallbackExplainer;
import com.finguard.common.enums.RiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiExplanationServiceTest {

    @Mock
    private DeterministicFallbackExplainer fallbackExplainer;

    @Mock
    private ChatModel chatModel;

    private ObjectMapper objectMapper = new ObjectMapper();

    private AiExplanationService service;

    @BeforeEach
    void setUp() {
        service = new AiExplanationService(fallbackExplainer, objectMapper, chatModel);
    }

    @Test
    @DisplayName("Should immediately invoke deterministic fallback when API key is placeholder")
    void shouldInvokeFallbackWhenKeyIsPlaceholder() {
        ReflectionTestUtils.setField(service, "apiKey", "your_openai_or_groq_api_key_here");

        AiExplanationRequest request = AiExplanationRequest.builder()
                .incidentId(UUID.randomUUID())
                .transactionId(UUID.randomUUID())
                .amount(new BigDecimal("1000.00"))
                .currency("USD")
                .riskScore(60)
                .riskLevel(RiskLevel.HIGH)
                .build();

        AiExplanationResponse expectedFallback = AiExplanationResponse.builder()
                .incidentId(request.getIncidentId())
                .transactionId(request.getTransactionId())
                .summary("Fallback explanation generated")
                .factors(List.of("Factor 1"))
                .recommendedActions(List.of("Action 1"))
                .isFallback(true)
                .modelUsed("DETERMINISTIC_RULES_FALLBACK_ENGINE")
                .generatedAt(Instant.now())
                .build();

        when(fallbackExplainer.generateFallbackExplanation(any())).thenReturn(expectedFallback);

        AiExplanationResponse result = service.generateExplanation(request);

        assertNotNull(result);
        assertTrue(result.isFallback());
        assertEquals("DETERMINISTIC_RULES_FALLBACK_ENGINE", result.getModelUsed());
        verify(fallbackExplainer).generateFallbackExplanation(request);
        verify(chatModel, never()).call(any(org.springframework.ai.chat.prompt.Prompt.class));
    }

    @Test
    @DisplayName("Should fall back cleanly when Spring AI chat model throws exception")
    void shouldFallbackWhenChatModelThrows() {
        ReflectionTestUtils.setField(service, "apiKey", "sk-proj-actualKeyProvided");
        ReflectionTestUtils.setField(service, "modelName", "gpt-4o-mini");

        AiExplanationRequest request = AiExplanationRequest.builder()
                .incidentId(UUID.randomUUID())
                .transactionId(UUID.randomUUID())
                .amount(new BigDecimal("1000.00"))
                .currency("USD")
                .riskScore(60)
                .riskLevel(RiskLevel.HIGH)
                .build();

        // Simulate network timeout or 429 rate limit
        when(chatModel.call(any(org.springframework.ai.chat.prompt.Prompt.class)))
                .thenThrow(new RuntimeException("Connection timed out to OpenAI API"));

        AiExplanationResponse fallback = AiExplanationResponse.builder()
                .incidentId(request.getIncidentId())
                .summary("Rule fallback after API error")
                .isFallback(true)
                .modelUsed("DETERMINISTIC_RULES_FALLBACK_ENGINE")
                .build();

        when(fallbackExplainer.generateFallbackExplanation(any())).thenReturn(fallback);

        AiExplanationResponse result = service.generateExplanation(request);

        assertNotNull(result);
        assertTrue(result.isFallback());
        verify(fallbackExplainer).generateFallbackExplanation(request);
    }
}
