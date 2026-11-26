package com.escapehub.evidence.infrastructure;

import com.escapehub.evidence.application.EvidenceRepository;
import com.escapehub.evidence.domain.EvidenceEntry;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("postgres")
public class JpaEvidenceRepository implements EvidenceRepository {

    private final SpringDataCollectedEvidenceRepository repository;

    public JpaEvidenceRepository(final SpringDataCollectedEvidenceRepository repository) {
        this.repository = repository;
    }

    @Override
    public EvidenceEntry save(final EvidenceEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("Entry must not be null");
        }
        return repository.save(new CollectedEvidenceEntity(entry)).toDomain();
    }

    @Override
    public List<EvidenceEntry> findBySessionId(final UUID sessionId) {
        return repository.findBySessionIdOrderByDiscoveredAtAsc(sessionId).stream()
                .map(CollectedEvidenceEntity::toDomain)
                .toList();
    }
}