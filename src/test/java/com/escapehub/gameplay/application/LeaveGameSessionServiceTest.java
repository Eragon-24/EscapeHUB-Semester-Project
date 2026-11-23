package com.escapehub.gameplay.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.escapehub.gameplay.domain.Game;
import com.escapehub.gameplay.domain.GameSession;
import com.escapehub.gameplay.domain.SessionParticipants;
import com.escapehub.gameplay.domain.movement.PlayerPosition;
import com.escapehub.gameplay.domain.movement.SessionPlayerPositions;
import com.escapehub.gameplay.infrastructure.InMemoryGameRepository;
import com.escapehub.gameplay.infrastructure.InMemoryGameSessionRepository;
import com.escapehub.gameplay.infrastructure.InMemorySessionParticipantsRepository;
import com.escapehub.gameplay.infrastructure.InMemorySessionPlayerPositionsRepository;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LeaveGameSessionServiceTest {

    private InMemoryGameSessionRepository sessionRepository;
    private InMemorySessionParticipantsRepository participantsRepository;
    private InMemorySessionPlayerPositionsRepository positionsRepository;
    private InMemoryGameRepository gameRepository;
    private CreateGameSessionService createSessionService;
    private JoinGameSessionService joinSessionService;
    private LeaveGameSessionService leaveSessionService;
    private GameSession session;

    @BeforeEach
    void setUp() {
        sessionRepository = new InMemoryGameSessionRepository();
        participantsRepository = new InMemorySessionParticipantsRepository();
        positionsRepository = new InMemorySessionPlayerPositionsRepository();
        gameRepository = new InMemoryGameRepository();
        createSessionService = new CreateGameSessionService(sessionRepository);
        joinSessionService = new JoinGameSessionService(sessionRepository, participantsRepository);
        leaveSessionService = new LeaveGameSessionService(
                sessionRepository, participantsRepository, positionsRepository);
        session = createSessionService.createSession("player1");
    }

    @Test
    void shouldRemoveOnlyLeavingParticipantAndAllowRejoiningWithSameCode() {
        joinSessionService.joinSession(session.joinCode(), "player2");
        final SessionParticipants participants = participantsRepository
                .findBySessionId(session.id())
                .orElseThrow();
        final SessionPlayerPositions positions = new SessionPlayerPositions(session.id());
        positions.updatePosition("player1", new PlayerPosition(480, 490));
        positions.updatePosition("player2", new PlayerPosition(480, 490));
        positionsRepository.save(positions);
        final Game game = new Game(session.id());
        gameRepository.save(game);

        leaveSessionService.leaveSession(session.joinCode().toLowerCase(), "player2");

        assertEquals(Set.of("player1"), participants.getPlayers());
        assertEquals(0, positions.getOtherPlayers("player1").size());
        assertEquals(1, positions.getOtherPlayers("player2").size());
        assertSame(game, gameRepository.findBySessionId(session.id()).orElseThrow());
        assertSame(session, sessionRepository.findById(session.id()).orElseThrow());

        final SessionParticipants rejoined = joinSessionService.joinSession(
                session.joinCode(), "player2");
        assertEquals(Set.of("player1", "player2"), rejoined.getPlayers());
    }

    @Test
    void shouldRejectLeavingAnotherUsersSessionWithoutChangingIt() {
        joinSessionService.joinSession(session.joinCode(), "player2");

        assertThrows(
                SecurityException.class,
                () -> leaveSessionService.leaveSession(session.joinCode(), "intruder")
        );

        final SessionParticipants participants = participantsRepository
                .findBySessionId(session.id())
                .orElseThrow();
        assertEquals(Set.of("player1", "player2"), participants.getPlayers());
    }

    @Test
    void shouldAllowCreatorToLeaveWithoutDeactivatingSession() {
        leaveSessionService.leaveSession(session.joinCode(), "player1");

        final SessionParticipants participants = participantsRepository
                .findBySessionId(session.id())
                .orElseThrow();
        assertFalse(participants.getPlayers().contains("player1"));
        assertTrue(participants.isActive());
        assertSame(session, sessionRepository.findById(session.id()).orElseThrow());

        assertEquals(
                Set.of("player2"),
                joinSessionService.joinSession(session.joinCode(), "player2").getPlayers()
        );
    }

    @Test
    void shouldRejectUnknownSession() {
        assertThrows(
                GameSessionNotFoundException.class,
                () -> leaveSessionService.leaveSession("ABC234", "player1")
        );
    }

    @Test
    void shouldMakeFreedSlotAvailableToAnotherParticipant() {
        joinSessionService.joinSession(session.joinCode(), "player2");
        joinSessionService.joinSession(session.joinCode(), "player3");
        joinSessionService.joinSession(session.joinCode(), "player4");
        joinSessionService.joinSession(session.joinCode(), "player5");
        joinSessionService.joinSession(session.joinCode(), "player6");

        leaveSessionService.leaveSession(session.joinCode(), "player2");

        final SessionParticipants participants = joinSessionService.joinSession(
                session.joinCode(), "player7");
        assertEquals(6, participants.getPlayers().size());
        assertFalse(participants.getPlayers().contains("player2"));
        assertTrue(participants.getPlayers().contains("player7"));
    }
}
