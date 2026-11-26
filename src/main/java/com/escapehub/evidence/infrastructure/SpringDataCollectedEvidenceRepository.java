package com.escapehub.evidence.infrastructure;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataCollectedEvidenceRepository
        extends JpaRepository<CollectedEvidenceEntity, UUID> {

    List<CollectedEvidenceEntity> findBySessionIdOrderByDiscoveredAtAsc(UUID sessionId);
}