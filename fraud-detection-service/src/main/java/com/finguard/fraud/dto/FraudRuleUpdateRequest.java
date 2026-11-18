package com.finguard.fraud.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudRuleUpdateRequest {

    @Min(value = 1, message = "Weight must be at least 1")
    @Max(value = 100, message = "Weight cannot exceed 100")
    private Integer weight;

    @DecimalMin(value = "0.00", message = "Threshold value must be non-negative")
    private BigDecimal thresholdValue;

    private Integer timeWindowSeconds;

    private Boolean isActive;
}
