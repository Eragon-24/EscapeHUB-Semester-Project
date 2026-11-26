package com.escapehub.evidence.application;

import com.escapehub.evidence.domain.EvidenceEntry;
import java.util.List;
import java.util.UUID;

public interface EvidenceRepository {
    EvidenceEntry save(EvidenceEntry entry);
    List<EvidenceEntry> findBySessionId(UUID sessionId);
}