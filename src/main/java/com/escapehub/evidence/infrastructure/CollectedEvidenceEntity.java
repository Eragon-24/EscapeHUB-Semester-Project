package com.escapehub.evidence.infrastructure;

import com.escapehub.evidence.domain.EvidenceEntry;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "collected_evidence")
public class CollectedEvidenceEntity {

    @Id
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

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
        this.title = entry.title();
        this.description = entry.description();
        this.discoveredAt = entry.discoveredAt();
    }

    public EvidenceEntry toDomain() {
        return new EvidenceEntry(id, sessionId, title, description, discoveredAt);
    }
}