package com.finguard.incident.repository;

import com.finguard.common.enums.IncidentStatus;
import com.finguard.incident.entity.FraudIncident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FraudIncidentRepository extends JpaRepository<FraudIncident, UUID> {
    Optional<FraudIncident> findByIncidentReference(String incidentReference);
    Optional<FraudIncident> findByTransactionId(UUID transactionId);
    boolean existsByTransactionId(UUID transactionId);
    Page<FraudIncident> findByStatus(IncidentStatus status, Pageable pageable);
    Page<FraudIncident> findByAssignedTo(UUID assignedTo, Pageable pageable);
    long countByStatus(IncidentStatus status);
}
