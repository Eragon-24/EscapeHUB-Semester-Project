package com.escapehub.evidence.infrastructure;

import com.escapehub.evidence.domain.EvidenceEntry;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "collected_evidence",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_collected_evidence_session_evidence_id",
                columnNames = {"session_id", "evidence_id"}
        )
)
public class CollectedEvidenceEntity {

    @Id
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "evidence_id", length = 100)
    private String evidenceId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(name = "discovered_at", nullable = false)
    private Instant discoveredAt;

    protected CollectedEvidenceEntity() {
    }

    public CollectedEvidenceEntity(final EvidenceEntry entry) {
        this.id = entry.id();
        this.sessionId = entry.sessionId();
        this.evidenceId = entry.evidenceId();
        this.title = entry.title();
        this.description = entry.description();
        this.discoveredAt = entry.discoveredAt();
    }

    public EvidenceEntry toDomain() {
        return new EvidenceEntry(id, sessionId, evidenceId, title, description, discoveredAt);
    }
}