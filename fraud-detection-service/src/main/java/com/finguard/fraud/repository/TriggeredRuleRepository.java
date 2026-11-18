package com.finguard.fraud.repository;

import com.finguard.fraud.entity.TriggeredRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TriggeredRuleRepository extends JpaRepository<TriggeredRuleEntity, UUID> {
    List<TriggeredRuleEntity> findByAssessmentId(UUID assessmentId);
}
