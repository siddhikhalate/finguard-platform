package com.finguard.ai.controller;

import com.finguard.ai.dto.AiExplanationRequest;
import com.finguard.ai.dto.AiExplanationResponse;
import com.finguard.ai.service.AiExplanationService;
import com.finguard.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Tag(name = "AI Fraud Explanation & Narrative", description = "Endpoints for generating and retrieving natural language fraud analysis summaries")
public class AiExplanationController {

    private final AiExplanationService aiExplanationService;

    @PostMapping("/explain")
    @Operation(summary = "Generate AI fraud explanation narrative",
               description = "Analyzes triggered fraud indicators with LLM (or deterministic fallback) to generate compliance briefings")
    public ResponseEntity<ApiResponse<AiExplanationResponse>> generateExplanation(
            @Valid @RequestBody AiExplanationRequest request) {

        AiExplanationResponse response = aiExplanationService.generateExplanation(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/incidents/{incidentId}")
    @Operation(summary = "Get explanation for a specific incident")
    public ResponseEntity<ApiResponse<AiExplanationResponse>> getExplanationByIncidentId(
            @PathVariable UUID incidentId) {

        AiExplanationResponse response = aiExplanationService.getExplanationByIncidentId(incidentId);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
