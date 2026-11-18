package com.finguard.fraud.controller;

import com.finguard.common.dto.ApiResponse;
import com.finguard.fraud.dto.FraudRuleUpdateRequest;
import com.finguard.fraud.dto.RiskAssessmentDto;
import com.finguard.fraud.entity.FraudRuleEntity;
import com.finguard.fraud.service.FraudDetectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fraud")
@RequiredArgsConstructor
@Tag(name = "Fraud Detection & Risk Scoring", description = "Endpoints for inspecting transaction risk assessments and managing fraud rules")
public class FraudDetectionController {

    private final FraudDetectionService fraudDetectionService;

    @GetMapping("/assessments/{transactionId}")
    @Operation(summary = "Get risk assessment for a specific transaction")
    public ResponseEntity<ApiResponse<RiskAssessmentDto>> getAssessmentByTransactionId(@PathVariable UUID transactionId) {
        RiskAssessmentDto assessment = fraudDetectionService.getAssessmentByTransactionId(transactionId);
        return ResponseEntity.ok(ApiResponse.ok(assessment));
    }

    @GetMapping("/assessments")
    @Operation(summary = "Get recent fraud assessments with pagination")
    public ResponseEntity<ApiResponse<Page<RiskAssessmentDto>>> getRecentAssessments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<RiskAssessmentDto> assessments = fraudDetectionService.getRecentAssessments(pageable);
        return ResponseEntity.ok(ApiResponse.ok(assessments));
    }

    @GetMapping("/rules")
    @Operation(summary = "List all configured fraud detection rules")
    public ResponseEntity<ApiResponse<List<FraudRuleEntity>>> getAllRules() {
        List<FraudRuleEntity> rules = fraudDetectionService.getAllRules();
        return ResponseEntity.ok(ApiResponse.ok(rules));
    }

    @PutMapping("/rules/{ruleId}")
    @Operation(summary = "Update fraud detection rule thresholds and status")
    public ResponseEntity<ApiResponse<FraudRuleEntity>> updateRule(
            @PathVariable UUID ruleId,
            @Valid @RequestBody FraudRuleUpdateRequest request) {
        FraudRuleEntity updatedRule = fraudDetectionService.updateRule(ruleId, request);
        return ResponseEntity.ok(ApiResponse.ok("Fraud rule updated successfully", updatedRule));
    }
}
