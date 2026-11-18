package com.finguard.incident.repository;

import com.finguard.incident.entity.AiExplanationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiExplanationRepository extends JpaRepository<AiExplanationEntity, UUID> {
    Optional<AiExplanationEntity> findByIncidentId(UUID incidentId);
}
