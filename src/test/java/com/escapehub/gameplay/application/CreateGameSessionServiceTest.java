package com.escapehub.gameplay.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.escapehub.gameplay.domain.GameSession;
import com.escapehub.gameplay.infrastructure.InMemoryGameSessionRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreateGameSessionServiceTest {

    private InMemoryGameSessionRepository repository;
    private CreateGameSessionService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryGameSessionRepository();
        service = new CreateGameSessionService(repository);
    }

    @Test
    void shouldCreateAndStoreSessionForAuthenticatedCreator() {
        final GameSession session = service.createSession("  playerOne  ");

        assertEquals("playerOne", session.creatorUsername());
        assertTrue(session.joinCode().matches("[A-HJ-NP-Z2-9]{6}"));
        assertTrue(repository.findById(session.id()).isPresent());
        assertEquals(session, repository.findByJoinCode(session.joinCode()).orElseThrow());
    }

    @Test
    void shouldGenerateDifferentJoinCodesForSeparateSessions() {
        final GameSession first = service.createSession("playerOne");
        final GameSession second = service.createSession("playerTwo");

        assertNotEquals(first.id(), second.id());
        assertNotEquals(first.joinCode(), second.joinCode());
    }

    @Test
    void shouldRejectMissingCreatorUsername() {
        assertThrows(IllegalArgumentException.class, () -> service.createSession(null));
        assertThrows(IllegalArgumentException.class, () -> service.createSession("   "));
        assertTrue(repository.findById(UUID.randomUUID()).isEmpty());
    }
}
