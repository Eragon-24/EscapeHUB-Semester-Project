package com.escapehub.evidence.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.escapehub.evidence.application.EvidenceRepository;
import com.escapehub.evidence.domain.EvidenceEntry;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.dao.DataIntegrityViolationException;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:evidence_jpa;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("postgres")
class JpaEvidenceRepositoryTest {

    @Autowired
    private EvidenceRepository repository;

    private EvidenceEntry entry(final UUID sessionId, final String title, final Instant at) {
        return new EvidenceEntry(UUID.randomUUID(), sessionId, title, "περιγραφή " + title, at);
    }

    @Test
    void postgresProfileUsesJpaAdapter() {
        assertInstanceOf(JpaEvidenceRepository.class, repository);
    }

    @Test
    void storesAndLoadsEntriesPerSessionOldestFirst() {
        final UUID mine = UUID.randomUUID();
        final UUID other = UUID.randomUUID();
        final Instant now = Instant.parse("2026-11-26T10:00:00Z");
        final EvidenceEntry second = entry(mine, "Σεντούκι", now.plusSeconds(5));
        final EvidenceEntry first = entry(mine, "Σημειωματάριο", now);
        repository.save(second);
        repository.save(first);
        repository.save(entry(other, "Άλλη ομάδα", now));

        final List<EvidenceEntry> result = repository.findBySessionId(mine);

        assertEquals(List.of(first, second), result);
        assertEquals(1, repository.findBySessionId(other).size());
        assertEquals(0, repository.findBySessionId(UUID.randomUUID()).size());
    }
    @Test
    void persistsEvidenceIdAndEnforcesUniqueSessionEvidenceIdConstraint() {
        final UUID sessionId = UUID.randomUUID();
        final EvidenceEntry first = new EvidenceEntry(
                UUID.randomUUID(),
                sessionId,
                "LAB_NOTEBOOK",
                "Σημειωματάριο Εργαστηρίου",
                "Περιγραφή (Πηγή: Πάγκος)",
                Instant.now()
        );
        final EvidenceEntry saved = repository.save(first);
        assertEquals("LAB_NOTEBOOK", saved.evidenceId());

        final List<EvidenceEntry> loaded = repository.findBySessionId(sessionId);
        assertEquals(1, loaded.size());
        assertEquals("LAB_NOTEBOOK", loaded.get(0).evidenceId());

        final EvidenceEntry duplicate = new EvidenceEntry(
                UUID.randomUUID(),
                sessionId,
                "LAB_NOTEBOOK",
                "Άλλος Τίτλος",
                "Άλλη Περιγραφή",
                Instant.now()
        );
        assertThrows(DataIntegrityViolationException.class, () -> repository.save(duplicate));
    }
}