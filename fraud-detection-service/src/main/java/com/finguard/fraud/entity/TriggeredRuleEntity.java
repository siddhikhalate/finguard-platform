package com.finguard.fraud.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "triggered_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TriggeredRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessment_id", nullable = false)
    @JsonIgnore
    private RiskAssessment assessment;

    @Column(name = "rule_code", nullable = false, length = 50)
    private String ruleCode;

    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;

    @Column(name = "points_added", nullable = false)
    private int pointsAdded;

    @Column(name = "trigger_reason", nullable = false, columnDefinition = "TEXT")
    private String triggerReason;
}
