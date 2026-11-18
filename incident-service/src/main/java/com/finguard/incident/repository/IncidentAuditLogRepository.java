package com.finguard.incident.repository;

import com.finguard.incident.entity.IncidentAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IncidentAuditLogRepository extends JpaRepository<IncidentAuditLog, UUID> {
    List<IncidentAuditLog> findByIncidentIdOrderByCreatedAtDesc(UUID incidentId);
}
