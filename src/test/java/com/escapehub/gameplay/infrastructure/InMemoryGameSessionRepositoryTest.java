package com.escapehub.gameplay.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.escapehub.gameplay.domain.GameSession;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemoryGameSessionRepositoryTest {

    private InMemoryGameSessionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryGameSessionRepository();
    }

    @Test
    void shouldFindSessionByIdAndJoinCode() {
        final GameSession session = createSession("ABC234");
        repository.save(session);

        assertEquals(session, repository.findById(session.id()).orElseThrow());
        assertEquals(session, repository.findByJoinCode("ABC234").orElseThrow());
        assertTrue(repository.existsByJoinCode("ABC234"));
    }

    @Test
    void shouldRejectDuplicateJoinCode() {
        repository.save(createSession("ABC234"));

        assertThrows(
                IllegalArgumentException.class,
                () -> repository.save(createSession("ABC234"))
        );
    }

    @Test
    void shouldReturnEmptyWhenSessionDoesNotExist() {
        assertTrue(repository.findById(UUID.randomUUID()).isEmpty());
        assertTrue(repository.findByJoinCode("ZZZZZZ").isEmpty());
        assertFalse(repository.existsByJoinCode("ZZZZZZ"));
    }

    private GameSession createSession(final String code) {
        return new GameSession(
                UUID.randomUUID(),
                code,
                "player",
                Instant.parse("2026-11-19T00:00:00Z")
        );
    }
}
