package com.escapehub.evidence.domain;

import java.text.Normalizer;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record EvidenceEntry(
        UUID id,
        UUID sessionId,
        String title,
        String description,
        Instant discoveredAt) {

    public EvidenceEntry {
        Objects.requireNonNull(id, "Entry id must not be null");
        Objects.requireNonNull(sessionId, "Session id must not be null");
        Objects.requireNonNull(title, "Title must not be null");
        Objects.requireNonNull(description, "Description must not be null");
        Objects.requireNonNull(discoveredAt, "Discovery time must not be null");
        title = title.trim();
        description = description.trim();
        if (title.isBlank()) {
            throw new IllegalArgumentException("Evidence title must not be blank");
        }
    }

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