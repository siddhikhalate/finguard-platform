package com.finguard.fraud.repository;

import com.finguard.common.enums.RuleType;
import com.finguard.fraud.entity.FraudRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FraudRuleRepository extends JpaRepository<FraudRuleEntity, UUID> {
    Optional<FraudRuleEntity> findByRuleCode(String ruleCode);
    List<FraudRuleEntity> findByIsActiveTrue();
    Optional<FraudRuleEntity> findByRuleTypeAndIsActiveTrue(RuleType ruleType);
}
