package com.finguard.incident.controller;

import com.finguard.common.constant.TracingConstants;
import com.finguard.common.dto.ApiResponse;
import com.finguard.common.enums.IncidentStatus;
import com.finguard.incident.dto.AssignIncidentRequest;
import com.finguard.incident.dto.IncidentDetailDto;
import com.finguard.incident.dto.IncidentSummaryDto;
import com.finguard.incident.dto.UpdateIncidentStatusRequest;
import com.finguard.incident.service.IncidentService;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incidents")
@RequiredArgsConstructor
@Tag(name = "Fraud Incident Management", description = "Endpoints for managing fraud investigation queues, analyst assignments, and case workflows")
public class IncidentController {

    private final IncidentService incidentService;

    @GetMapping
    @Operation(summary = "Get list of fraud incidents with filtering and pagination")
    public ResponseEntity<ApiResponse<Page<IncidentSummaryDto>>> getIncidents(
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<IncidentSummaryDto> incidents = incidentService.getIncidents(status, pageable);
        return ResponseEntity.ok(ApiResponse.ok(incidents));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get detailed fraud incident investigation dossier")
    public ResponseEntity<ApiResponse<IncidentDetailDto>> getIncidentById(@PathVariable UUID id) {
        IncidentDetailDto detail = incidentService.getIncidentById(id);
        return ResponseEntity.ok(ApiResponse.ok(detail));
    }

    @PutMapping("/{id}/assign")
    @Operation(summary = "Assign incident to a compliance analyst")
    public ResponseEntity<ApiResponse<IncidentSummaryDto>> assignIncident(
            @PathVariable UUID id,
            @Valid @RequestBody AssignIncidentRequest request,
            @RequestHeader(value = TracingConstants.USERNAME_HEADER, required = false) String actorUsername) {

        IncidentSummaryDto updated = incidentService.assignIncident(id, request, actorUsername);
        return ResponseEntity.ok(ApiResponse.ok("Incident successfully assigned to analyst", updated));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Transition incident investigation status")
    public ResponseEntity<ApiResponse<IncidentSummaryDto>> updateIncidentStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateIncidentStatusRequest request,
            @RequestHeader(value = TracingConstants.USER_ID_HEADER, required = false) String actorIdStr,
            @RequestHeader(value = TracingConstants.USERNAME_HEADER, required = false) String actorUsername) {

        UUID actorId = (actorIdStr != null && !actorIdStr.isBlank()) ? UUID.fromString(actorIdStr) : null;
        IncidentSummaryDto updated = incidentService.updateIncidentStatus(id, request, actorId, actorUsername);
        return ResponseEntity.ok(ApiResponse.ok("Incident status updated successfully", updated));
    }
}
