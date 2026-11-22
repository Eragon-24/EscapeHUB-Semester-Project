package com.escapehub.gameplay.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SharedGameStateTest {

    private static final Instant NOW = Instant.parse("2026-11-22T10:00:00Z");

    private final UUID sessionId = UUID.randomUUID();
    private final SharedGameState state = new SharedGameState(sessionId);

    @Test
    void newStateIsEmptyAtVersionZero() {
        final SharedStateSnapshot snapshot = state.snapshot();

        assertEquals(0, snapshot.version());
        assertTrue(snapshot.values().isEmpty());
        assertEquals(sessionId, snapshot.sessionId());
    }

    @Test
    void everyChangeIncreasesTheVersionByOne() {
        final SharedStateUpdate first = state.apply("door", "open", "alice", NOW);
        final SharedStateUpdate second = state.apply("lamp", "on", "bob", NOW);

        assertEquals(1, first.version());
        assertEquals(2, second.version());
        assertEquals(2, state.getVersion());
    }

    @Test
    void updateCarriesTheChangeAndItsAuthor() {
        final SharedStateUpdate update = state.apply("door", "open", "alice", NOW);

        assertEquals(sessionId, update.sessionId());
        assertEquals("door", update.key());
        assertEquals("open", update.value());
        assertEquals("alice", update.author());
        assertEquals(NOW, update.at());
    }

    @Test
    void laterChangeOfTheSameKeyReplacesTheValue() {
        state.apply("door", "closed", "alice", NOW);
        state.apply("door", "open", "bob", NOW);

        assertEquals("open", state.snapshot().values().get("door"));
        assertEquals(1, state.snapshot().values().size());
    }

    @Test
    void snapshotIsAnIndependentImmutableCopy() {
        state.apply("door", "closed", "alice", NOW);
        final SharedStateSnapshot before = state.snapshot();
        state.apply("door", "open", "alice", NOW);

        assertEquals("closed", before.values().get("door"));
        assertThrows(UnsupportedOperationException.class, () -> before.values().put("x", "y"));
    }

    @Test
    void sessionIdIsRequired() {
        assertThrows(NullPointerException.class, () -> new SharedGameState(null));
    }
}
