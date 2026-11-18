package com.finguard.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finguard.ai.dto.AiExplanationRequest;
import com.finguard.ai.dto.AiExplanationResponse;
import com.finguard.ai.fallback.DeterministicFallbackExplainer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class AiExplanationService {

    private final DeterministicFallbackExplainer fallbackExplainer;
    private final ObjectMapper objectMapper;
    private final ChatModel chatModel;

    @Value("${spring.ai.openai.api-key:your_openai_or_groq_api_key_here}")
    private String apiKey;

    @Value("${spring.ai.openai.chat.options.model:gpt-4o-mini}")
    private String modelName;

    // Thread-safe in-memory cache for recent AI explanations
    private final Map<UUID, AiExplanationResponse> explanationCache = new ConcurrentHashMap<>();

    @Autowired
    public AiExplanationService(
            DeterministicFallbackExplainer fallbackExplainer,
            ObjectMapper objectMapper,
            @Autowired(required = false) ChatModel chatModel) {
        this.fallbackExplainer = fallbackExplainer;
        this.objectMapper = objectMapper;
        this.chatModel = chatModel;
    }

    public AiExplanationResponse generateExplanation(AiExplanationRequest request) {
        // If API key is unconfigured or a placeholder, use deterministic fallback immediately
        if (!isAiProviderConfigured() || chatModel == null) {
            log.info("AI Provider not configured or using placeholder key. Activating Deterministic Fallback Engine.");
            AiExplanationResponse response = fallbackExplainer.generateFallbackExplanation(request);
            cacheExplanation(response);
            return response;
        }

        try {
            log.info("Calling Spring AI ({}) to generate natural language explanation for txn {}",
                    modelName, request.getTransactionId());

            String systemPrompt = """
                You are a Senior Financial Crime Compliance & Fraud Risk Analyst at an international bank.
                Analyze the following flagged financial transaction based on deterministic fraud rules triggered.
                Provide a structured, professional, executive briefing for human compliance officers.
                Do NOT make final financial decisions; assist the human analyst.
                
                Respond in valid JSON format matching this schema:
                {
                  "summary": "Brief 2-3 sentence overview explaining why this transaction was flagged",
                  "factors": ["Key risk factor 1", "Key risk factor 2"],
                  "recommendedActions": ["Action 1", "Action 2", "Action 3"],
                  "confidenceLevel": "HIGH / MEDIUM / LOW"
                }
                """;

            String userPrompt = String.format("""
                Transaction ID: %s
                Account ID: %s
                Amount: %s %s
                Location: %s
                Risk Score: %d / 100
                Risk Level: %s
                Triggered Rules: %s
                """,
                    request.getTransactionId(),
                    request.getAccountId(),
                    request.getAmount(),
                    request.getCurrency(),
                    request.getLocation(),
                    request.getRiskScore(),
                    request.getRiskLevel(),
                    request.getTriggeredRules()
            );

            Prompt prompt = new Prompt(systemPrompt + "\n\n" + userPrompt);
            String aiRawResponse = chatModel.call(prompt).getResult().getOutput().getContent();

            // Clean any potential markdown JSON wrappers
            String jsonPayload = cleanJsonMarkdown(aiRawResponse);
            com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>> typeRef = 
                    new com.fasterxml.jackson.core.type.TypeReference<>() {};
            Map<String, Object> parsed = objectMapper.readValue(jsonPayload, typeRef);

            @SuppressWarnings("unchecked")
            List<String> factors = (List<String>) parsed.get("factors");
            @SuppressWarnings("unchecked")
            List<String> actions = (List<String>) parsed.get("recommendedActions");

            String confidenceLevel = parsed.get("confidenceLevel") != null 
                    ? parsed.get("confidenceLevel").toString() 
                    : "HIGH";

            AiExplanationResponse response = AiExplanationResponse.builder()
                    .incidentId(request.getIncidentId())
                    .transactionId(request.getTransactionId())
                    .summary((String) parsed.get("summary"))
                    .factors(factors != null ? factors : List.of())
                    .recommendedActions(actions != null ? actions : List.of())
                    .confidenceLevel(confidenceLevel)
                    .modelUsed(modelName)
                    .isFallback(false)
                    .generatedAt(Instant.now())
                    .build();

            cacheExplanation(response);
            return response;

        } catch (Exception ex) {
            log.warn("Spring AI invocation failed: {}. Seamlessly falling back to Deterministic Rules Engine.", ex.getMessage());
            AiExplanationResponse fallback = fallbackExplainer.generateFallbackExplanation(request);
            cacheExplanation(fallback);
            return fallback;
        }
    }

    public AiExplanationResponse getExplanationByIncidentId(UUID incidentId) {
        return explanationCache.get(incidentId);
    }

    private boolean isAiProviderConfigured() {
        return StringUtils.hasText(apiKey) &&
                !apiKey.equalsIgnoreCase("your_openai_or_groq_api_key_here") &&
                !apiKey.isBlank();
    }

    private void cacheExplanation(AiExplanationResponse response) {
        if (response.getIncidentId() != null) {
            explanationCache.put(response.getIncidentId(), response);
        }
        if (response.getTransactionId() != null) {
            explanationCache.put(response.getTransactionId(), response);
        }
    }

    private String cleanJsonMarkdown(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }
}
