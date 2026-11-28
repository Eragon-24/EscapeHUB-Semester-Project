package com.escapehub.evidence.infrastructure;

import com.escapehub.evidence.application.EvidenceRepository;
import com.escapehub.evidence.domain.EvidenceEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!postgres")
public class InMemoryEvidenceRepository implements EvidenceRepository {

    private final Map<UUID, List<EvidenceEntry>> entriesBySession = new HashMap<>();

    @Override
    public synchronized EvidenceEntry save(final EvidenceEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("Entry must not be null");
        }
        final List<EvidenceEntry> sessionEntries =
                entriesBySession.computeIfAbsent(entry.sessionId(), id -> new ArrayList<>());
        if (entry.evidenceId() != null) {
            final boolean duplicate = sessionEntries.stream()
                    .anyMatch(existing -> entry.evidenceId().equals(existing.evidenceId()));
            if (duplicate) {
                throw new DataIntegrityViolationException(
                        "Duplicate evidenceId for session: " + entry.evidenceId());
            }
        }
        sessionEntries.add(entry);
        return entry;
    }

    @Override
    public synchronized List<EvidenceEntry> findBySessionId(final UUID sessionId) {
        return entriesBySession.getOrDefault(sessionId, List.of()).stream()
                .sorted(Comparator.comparing(EvidenceEntry::discoveredAt))
                .toList();
    }
}