package com.finguard.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Details of a specific fraud detection rule that triggered during transaction evaluation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriggeredRuleDto implements Serializable {

    private String ruleCode;
    private String ruleName;
    private int pointsAdded;
    private String triggerReason;
}
