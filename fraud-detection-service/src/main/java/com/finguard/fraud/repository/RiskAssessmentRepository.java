package com.finguard.fraud.repository;

import com.finguard.fraud.entity.RiskAssessment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, UUID> {
    Optional<RiskAssessment> findByTransactionId(UUID transactionId);
    Page<RiskAssessment> findAllByOrderByEvaluationTimestampDesc(Pageable pageable);
    boolean existsByTransactionId(UUID transactionId);
}
