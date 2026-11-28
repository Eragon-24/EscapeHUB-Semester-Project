package com.escapehub.evidence.domain;

import java.text.Normalizer;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Shared journal entry representing a discovered clue within a game session.
 *
 * @param id unique entry identifier
 * @param sessionId owning game session identifier
 * @param evidenceId catalog identifier of the unique clue (nullable for legacy entries)
 * @param title display title of the clue
 * @param description descriptive content of the clue
 * @param discoveredAt timestamp when the clue was discovered
 */
public record EvidenceEntry(
        UUID id,
        UUID sessionId,
        String evidenceId,
        String title,
        String description,
        Instant discoveredAt) {

    /**
     * Validates required fields and normalizes text attributes.
     */
    public EvidenceEntry {
        Objects.requireNonNull(id, "Entry id must not be null");
        Objects.requireNonNull(sessionId, "Session id must not be null");
        Objects.requireNonNull(title, "Title must not be null");
        Objects.requireNonNull(description, "Description must not be null");
        Objects.requireNonNull(discoveredAt, "Discovery time must not be null");
        evidenceId = evidenceId == null || evidenceId.isBlank()
                ? null
                : evidenceId.trim().toUpperCase(Locale.ROOT);
        title = title.trim();
        description = description.trim();
        if (title.isBlank()) {
            throw new IllegalArgumentException("Evidence title must not be blank");
        }
    }

    /**
     * Backward-compatible constructor without an explicit catalog {@code evidenceId}.
     *
     * @param id unique entry identifier
     * @param sessionId owning game session identifier
     * @param title display title of the clue
     * @param description descriptive content of the clue
     * @param discoveredAt timestamp when the clue was discovered
     */
    public EvidenceEntry(
            final UUID id,
            final UUID sessionId,
            final String title,
            final String description,
            final Instant discoveredAt) {
        this(id, sessionId, null, title, description, discoveredAt);
    }

    /**
     * Checks whether this entry matches a case- and accent-insensitive search query.
     *
     * @param query optional search term
     * @return true when query is blank or contained in title or description
     */
    public boolean matches(final String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        final String needle = normalize(query);
        return normalize(title).contains(needle) || normalize(description).contains(needle);
    }

    private static String normalize(final String text) {
        return Normalizer.normalize(text.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replace('ς', 'σ');
    }
}