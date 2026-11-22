package com.escapehub.gameplay.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.escapehub.gameplay.domain.GameSession;
import com.escapehub.gameplay.domain.SessionParticipants;
import com.escapehub.gameplay.infrastructure.InMemoryGameSessionRepository;
import com.escapehub.gameplay.infrastructure.InMemorySessionParticipantsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JoinGameSessionServiceTest {

    private CreateGameSessionService createService;
    private JoinGameSessionService joinService;
    private InMemorySessionParticipantsRepository participantsRepository;

    @BeforeEach
    void setUp() {
        final InMemoryGameSessionRepository sessionRepository = new InMemoryGameSessionRepository();
        participantsRepository = new InMemorySessionParticipantsRepository();
        createService = new CreateGameSessionService(sessionRepository);
        joinService = new JoinGameSessionService(sessionRepository, participantsRepository);
    }

    @Test
    void shouldAllowUpToSixPlayersAndRejectSeventhPlayer() {
        final GameSession session = createService.createSession("player1");

        joinService.joinSession(session.joinCode().toLowerCase(), "player2");
        joinService.joinSession(session.joinCode(), "player3");
        joinService.joinSession(session.joinCode(), "player4");
        joinService.joinSession(session.joinCode(), "player5");
        final SessionParticipants full = joinService.joinSession(session.joinCode(), "player6");

        assertEquals(6, full.getPlayers().size());
        assertTrue(full.getPlayers().contains("player1"));
        assertTrue(full.getPlayers().contains("player6"));

        assertThrows(
                IllegalStateException.class,
                () -> joinService.joinSession(session.joinCode(), "player7")
        );
    }

    @Test
    void shouldRejectInvalidJoinCode() {
        assertThrows(
                IllegalArgumentException.class,
                () -> joinService.joinSession("ZZZZZZ", "player2")
        );
    }

    @Test
    void shouldRejectJoiningInactiveSession() {
        final GameSession session = createService.createSession("player1");
        final SessionParticipants participants = joinService.joinSession(
                session.joinCode(), "player2");
        participants.deactivate();
        participantsRepository.save(participants);

        assertThrows(
                IllegalStateException.class,
                () -> joinService.joinSession(session.joinCode(), "player3")
        );
    }
}