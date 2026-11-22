package com.escapehub.gameplay.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.escapehub.gameplay.domain.GameSession;
import com.escapehub.gameplay.domain.SessionParticipants;
import com.escapehub.gameplay.domain.movement.OtherPlayerView;
import com.escapehub.gameplay.domain.movement.PlayerPosition;
import com.escapehub.gameplay.infrastructure.InMemoryGameSessionRepository;
import com.escapehub.gameplay.infrastructure.InMemorySessionParticipantsRepository;
import com.escapehub.gameplay.infrastructure.InMemorySessionPlayerPositionsRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OtherPlayersMovementServiceTest {

    private CreateGameSessionService createSessionService;
    private JoinGameSessionService joinSessionService;
    private InMemorySessionParticipantsRepository participantsRepository;
    private OtherPlayersMovementService service;
    private GameSession session;

    @BeforeEach
    void setUp() {
        final InMemoryGameSessionRepository sessionRepository = new InMemoryGameSessionRepository();
        participantsRepository = new InMemorySessionParticipantsRepository();
        createSessionService = new CreateGameSessionService(sessionRepository);
        joinSessionService = new JoinGameSessionService(sessionRepository, participantsRepository);
        service = new OtherPlayersMovementService(
                new InMemorySessionPlayerPositionsRepository(),
                sessionRepository,
                participantsRepository
        );
        session = createSessionService.createSession("player1");
        joinSessionService.joinSession(session.joinCode(), "player2");
        joinSessionService.joinSession(session.joinCode(), "player3");
    }

    @Test
    void shouldReturnOnlyOtherPlayersWithTheirLatestPositions() {
        service.updatePlayerPosition(session.id(), "player1", new PlayerPosition(480, 490));
        service.updatePlayerPosition(session.id(), "player2", new PlayerPosition(300, 250));
        service.updatePlayerPosition(session.id(), "player2", new PlayerPosition(320, 260));
        service.updatePlayerPosition(session.id(), "player3", new PlayerPosition(600, 400));

        final List<OtherPlayerView> othersForPlayer1 = service
                .getOtherPlayersPositions(session.id(), "player1");

        assertEquals(2, othersForPlayer1.size());
        assertEquals(
                new OtherPlayerView("player2", new PlayerPosition(320, 260)),
                othersForPlayer1.get(0)
        );
        assertEquals(
                new OtherPlayerView("player3", new PlayerPosition(600, 400)),
                othersForPlayer1.get(1)
        );
    }

    @Test
    void shouldRemovePlayerFromVisibleOthersWhenLeaving() {
        service.updatePlayerPosition(session.id(), "player1", new PlayerPosition(480, 490));
        service.updatePlayerPosition(session.id(), "player2", new PlayerPosition(300, 250));

        service.removePlayer(session.id(), "player2");

        assertTrue(service.getOtherPlayersPositions(session.id(), "player1").isEmpty());
    }

    @Test
    void shouldRejectNonParticipantAccessAndUpdates() {
        assertThrows(
                SecurityException.class,
                () -> service.getOtherPlayersPositions(session.id(), "intruder")
        );
        assertThrows(
                SecurityException.class,
                () -> service.updatePlayerPosition(
                        session.id(), "intruder", new PlayerPosition(100, 100))
        );
    }

    @Test
    void shouldRejectUnknownSession() {
        assertThrows(
                NoSuchElementException.class,
                () -> service.getOtherPlayersPositions(UUID.randomUUID(), "player1")
        );
    }

    @Test
    void shouldRejectInactiveSession() {
        final SessionParticipants participants = participantsRepository
                .findBySessionId(session.id())
                .orElseThrow();
        participants.deactivate();
        participantsRepository.save(participants);

        assertThrows(
                IllegalStateException.class,
                () -> service.getOtherPlayersPositions(session.id(), "player1")
        );
    }
}