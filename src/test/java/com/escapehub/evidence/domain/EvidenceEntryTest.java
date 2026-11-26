package com.escapehub.evidence.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EvidenceEntryTest {

    private EvidenceEntry entry(final String title, final String description) {
        return new EvidenceEntry(
                UUID.randomUUID(), UUID.randomUUID(), title, description, Instant.now());
    }

    @Test
    void matchesTitleOrDescriptionIgnoringCaseAndAccents() {
        final EvidenceEntry e = entry("Παλιό Σημειωματάριο", "Κωδικός στην τελευταία σελίδα");

        assertTrue(e.matches("σημειωματαριο"));
        assertTrue(e.matches("ΤΕΛΕΥΤΑΙΑ"));
        assertTrue(e.matches("  "));
        assertTrue(e.matches(null));
        assertFalse(e.matches("σεντούκι"));
    }

    @Test
    void rejectsBlankTitle() {
        assertThrows(IllegalArgumentException.class, () -> entry(" ", "x"));
    }
}
