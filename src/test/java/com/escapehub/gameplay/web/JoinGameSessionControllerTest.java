package com.escapehub.gameplay.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.escapehub.gameplay.application.GameStateService;
import com.escapehub.gameplay.application.JoinGameSessionService;
import com.escapehub.gameplay.domain.Game;
import com.escapehub.gameplay.domain.GameState;
import com.escapehub.gameplay.domain.GameStatus;
import com.escapehub.gameplay.domain.SessionParticipants;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

class JoinGameSessionControllerTest {

    @Test
    void shouldJoinSessionForAuthenticatedUserAndPopulateFlashAttributes() {
        final JoinGameSessionService service = mock(JoinGameSessionService.class);
        final GameStateService gameStateService = mock(GameStateService.class);
        final JoinGameSessionController controller = new JoinGameSessionController(
                service, gameStateService);
        final SessionParticipants participants = new SessionParticipants(
                UUID.randomUUID(), "player1");
        participants.addPlayer("player2");
        final RedirectAttributes attributes = new RedirectAttributesModelMap();

        final GameState state = new GameState(
                participants.getSessionId(), Game.INITIAL_ROOM_ID, GameStatus.IN_PROGRESS,
                Map.of(), 0);
        when(service.joinSession("abc234", "player2")).thenReturn(participants);
        when(gameStateService.enterGame("ABC234", "player2")).thenReturn(state);

        final String result = controller.joinGameSession("abc234", () -> "player2", attributes);

        assertEquals("redirect:/multiplayer-room", result);
        assertEquals("ABC234", attributes.getFlashAttributes().get("joinedSessionCode"));
        assertEquals(
                participants.getPlayers(),
                attributes.getFlashAttributes().get("sessionPlayers")
        );
        assertEquals(
                participants.getSessionId().toString(),
                attributes.getFlashAttributes().get("joinedSessionId")
        );
        assertEquals("ABC234", attributes.asMap().get("session"));
        assertEquals(
                participants.getSessionId().toString(),
                attributes.asMap().get("sessionId")
        );
        verify(service).joinSession("abc234", "player2");
        verify(gameStateService).enterGame("ABC234", "player2");
    }

    @Test
    void shouldRedirectAnonymousUserToLogin() {
        final JoinGameSessionService service = mock(JoinGameSessionService.class);
        final GameStateService gameStateService = mock(GameStateService.class);
        final JoinGameSessionController controller = new JoinGameSessionController(service, gameStateService);

        final String result = controller.joinGameSession(
                "ABC234",
                () -> "anonymousUser",
                new RedirectAttributesModelMap()
        );

        assertEquals("redirect:/login", result);
        verifyNoInteractions(service);
    }

    @Test
    void shouldReturnErrorFlashAttributeWhenJoinFails() {
        final JoinGameSessionService service = mock(JoinGameSessionService.class);
        final GameStateService gameStateService = mock(GameStateService.class);
        final JoinGameSessionController controller = new JoinGameSessionController(service, gameStateService);
        final RedirectAttributes attributes = new RedirectAttributesModelMap();

        when(service.joinSession("ZZZZZZ", "player2"))
                .thenThrow(new IllegalArgumentException("Μη έγκυρος κωδικός συνεδρίας."));

        final String result = controller.joinGameSession("ZZZZZZ", () -> "player2", attributes);

        assertEquals("redirect:/multiplayer-room", result);
        assertEquals(
                "Μη έγκυρος κωδικός συνεδρίας.",
                attributes.getFlashAttributes().get("joinErrorMessage")
        );
    }
}