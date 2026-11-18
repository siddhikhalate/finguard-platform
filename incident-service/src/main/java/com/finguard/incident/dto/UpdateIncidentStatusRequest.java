package com.finguard.incident.dto;

import com.finguard.common.enums.IncidentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateIncidentStatusRequest {

    @NotNull(message = "New incident status is required")
    private IncidentStatus status;

    private String notes;
}
