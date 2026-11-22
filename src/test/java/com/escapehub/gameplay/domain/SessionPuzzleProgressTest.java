package com.escapehub.gameplay.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SessionPuzzleProgressTest {

    private static final Instant NOW = Instant.parse("2026-11-22T10:00:00Z");

    private final UUID sessionId = UUID.randomUUID();
    private final SessionPuzzleProgress progress = new SessionPuzzleProgress(sessionId);

    @Test
    void startsWithNothingSolved() {
        assertFalse(progress.isSolved("chest"));
        assertTrue(progress.solvedPuzzleIds().isEmpty());
        assertEquals(sessionId, progress.getSessionId());
    }

    @Test
    void markingSolvedReturnsTrueOnlyTheFirstTime() {
        assertTrue(progress.markSolved("chest", "alice", NOW));
        assertFalse(progress.markSolved("chest", "bob", NOW.plusSeconds(5)));

        assertTrue(progress.isSolved("chest"));
        assertEquals(Set.of("chest"), progress.solvedPuzzleIds());
    }

    @Test
    void otherPuzzlesAreIndependent() {
        progress.markSolved("chest", "alice", NOW);

        assertFalse(progress.isSolved("door"));
        assertTrue(progress.markSolved("door", "bob", NOW));
    }

    @Test
    void solvedIdsCannotBeModifiedFromOutside() {
        progress.markSolved("chest", "alice", NOW);

        assertThrows(UnsupportedOperationException.class, () -> progress.solvedPuzzleIds().add("x"));
    }

    @Test
    void sessionIdIsRequired() {
        assertThrows(NullPointerException.class, () -> new SessionPuzzleProgress(null));
    }
}
