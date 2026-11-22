package com.escapehub.gameplay.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.escapehub.gameplay.domain.movement.OtherPlayerView;
import com.escapehub.gameplay.domain.movement.PlayerPosition;
import com.escapehub.gameplay.infrastructure.InMemorySessionPlayerPositionsRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OtherPlayersMovementServiceTest {

    private OtherPlayersMovementService service;
    private UUID sessionId;

    @BeforeEach
    void setUp() {
        service = new OtherPlayersMovementService(new InMemorySessionPlayerPositionsRepository());
        sessionId = UUID.randomUUID();
    }

    @Test
    void shouldReturnOnlyOtherPlayersWithTheirLatestPositions() {
        service.updatePlayerPosition(sessionId, "player1", new PlayerPosition(480, 490));
        service.updatePlayerPosition(sessionId, "player2", new PlayerPosition(300, 250));
        service.updatePlayerPosition(sessionId, "player2", new PlayerPosition(320, 260));
        service.updatePlayerPosition(sessionId, "player3", new PlayerPosition(600, 400));

        final List<OtherPlayerView> othersForPlayer1 = service
                .getOtherPlayersPositions(sessionId, "player1");

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
        service.updatePlayerPosition(sessionId, "player1", new PlayerPosition(480, 490));
        service.updatePlayerPosition(sessionId, "player2", new PlayerPosition(300, 250));

        service.removePlayer(sessionId, "player2");

        assertTrue(service.getOtherPlayersPositions(sessionId, "player1").isEmpty());
    }

    @Test
    void shouldReturnEmptyListForUnknownSession() {
        assertTrue(service.getOtherPlayersPositions(UUID.randomUUID(), "player1").isEmpty());
    }

    @Test
    void shouldRejectBlankViewerUsername() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.getOtherPlayersPositions(sessionId, "   ")
        );
    }
}