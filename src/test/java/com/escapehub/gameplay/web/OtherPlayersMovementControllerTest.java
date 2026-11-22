package com.escapehub.gameplay.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.escapehub.gameplay.application.OtherPlayersMovementService;
import com.escapehub.gameplay.domain.movement.OtherPlayerView;
import com.escapehub.gameplay.domain.movement.PlayerPosition;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class OtherPlayersMovementControllerTest {

    @Test
    void shouldReturnOtherPlayersForAuthenticatedUser() {
        final OtherPlayersMovementService service = mock(OtherPlayersMovementService.class);
        final OtherPlayersMovementController controller = new OtherPlayersMovementController(
                service);
        final UUID sessionId = UUID.randomUUID();
        final List<OtherPlayerView> expected = List.of(
                new OtherPlayerView("player2", new PlayerPosition(320, 260))
        );

        when(service.getOtherPlayersPositions(sessionId, "player1")).thenReturn(expected);

        final ResponseEntity<List<OtherPlayerView>> response = controller.getOtherPlayers(
                sessionId, () -> "player1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(service).getOtherPlayersPositions(sessionId, "player1");
    }

    @Test
    void shouldReturnUnauthorizedForAnonymousVisitor() {
        final OtherPlayersMovementService service = mock(OtherPlayersMovementService.class);
        final OtherPlayersMovementController controller = new OtherPlayersMovementController(
                service);

        final ResponseEntity<List<OtherPlayerView>> response = controller.getOtherPlayers(
                UUID.randomUUID(), () -> "anonymousUser");

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(service);
    }
}