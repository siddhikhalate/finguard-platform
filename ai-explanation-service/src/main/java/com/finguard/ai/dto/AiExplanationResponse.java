package com.finguard.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiExplanationResponse {

    private UUID incidentId;
    private UUID transactionId;
    private String summary;
    private List<String> factors;
    private List<String> recommendedActions;
    private String confidenceLevel;
    private String modelUsed;
    private boolean isFallback;
    @Builder.Default
    private Instant generatedAt = Instant.now();
}
