package com.escapehub.gameplay.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.escapehub.gameplay.application.CreateGameSessionService;
import com.escapehub.gameplay.application.GameStateService;
import com.escapehub.gameplay.application.LeaveGameSessionService;
import com.escapehub.gameplay.domain.Game;
import com.escapehub.gameplay.domain.GameSession;
import com.escapehub.gameplay.domain.GameState;
import com.escapehub.gameplay.domain.GameStatus;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

class GameSessionControllerTest {

    @Test
    void shouldCreateSessionForAuthenticatedPrincipalAndShowInviteCode() {
        final CreateGameSessionService service = mock(CreateGameSessionService.class);
        final GameStateService gameStateService = mock(GameStateService.class);
        final GameSessionController controller = new GameSessionController(
                service, gameStateService, mock(LeaveGameSessionService.class));
        final GameSession session = new GameSession(
                UUID.randomUUID(),
                "ABC234",
                "playerOne",
                Instant.parse("2026-11-19T00:00:00Z")
        );
        final RedirectAttributes attributes = new RedirectAttributesModelMap();
        final GameState state = new GameState(
                session.id(), Game.INITIAL_ROOM_ID, GameStatus.IN_PROGRESS, Map.of(),
                java.util.Set.of(), java.util.Set.of(), false, 0);
        when(service.createSession("playerOne")).thenReturn(session);
        when(gameStateService.enterGame("ABC234", "playerOne")).thenReturn(state);

        final String result = controller.createGameSession(() -> "playerOne", attributes);

        assertEquals("redirect:/multiplayer-room?session=ABC234", result);
        assertEquals("ABC234", attributes.getFlashAttributes().get("createdSessionCode"));
        assertEquals(session.id().toString(), attributes.getFlashAttributes().get("createdSessionId"));
        verify(service).createSession("playerOne");
        verify(gameStateService).enterGame("ABC234", "playerOne");
    }

    @Test
    void shouldRedirectAnonymousVisitorToLoginWithoutCreatingSession() {
        final CreateGameSessionService service = mock(CreateGameSessionService.class);
        final GameStateService gameStateService = mock(GameStateService.class);
        final GameSessionController controller = new GameSessionController(
                service, gameStateService, mock(LeaveGameSessionService.class));

        final String result = controller.createGameSession(
                () -> "anonymousUser",
                new RedirectAttributesModelMap()
        );

        assertEquals("redirect:/login", result);
        verifyNoInteractions(service);
    }

    @Test
    void shouldRedirectWhenPrincipalIsMissing() {
        final CreateGameSessionService service = mock(CreateGameSessionService.class);
        final GameStateService gameStateService = mock(GameStateService.class);
        final GameSessionController controller = new GameSessionController(
                service, gameStateService, mock(LeaveGameSessionService.class));

        assertEquals(
                "redirect:/login",
                controller.createGameSession(null, new RedirectAttributesModelMap())
        );
        verifyNoInteractions(service);
    }

    @Test
    void shouldLeaveSessionUsingAuthenticatedPrincipalAndRedirectWithConfirmation() {
        final LeaveGameSessionService leaveService = mock(LeaveGameSessionService.class);
        final GameSessionController controller = new GameSessionController(
                mock(CreateGameSessionService.class),
                mock(GameStateService.class),
                leaveService
        );
        final RedirectAttributes attributes = new RedirectAttributesModelMap();

        final String result = controller.leaveGameSession(
                "ABC234", () -> "playerOne", attributes);

        assertEquals("redirect:/multiplayer-room", result);
        assertEquals(
                "Αποχώρησες από τη συνεδρία. Το παιχνίδι συνεχίζεται για τους υπόλοιπους.",
                attributes.getFlashAttributes().get("leaveSessionMessage")
        );
        assertEquals(true, attributes.getFlashAttributes().get("sessionLeft"));
        verify(leaveService).leaveSession("ABC234", "playerOne");
    }

    @Test
    void shouldRejectAnonymousLeaveWithoutCallingService() {
        final LeaveGameSessionService leaveService = mock(LeaveGameSessionService.class);
        final GameSessionController controller = new GameSessionController(
                mock(CreateGameSessionService.class),
                mock(GameStateService.class),
                leaveService
        );

        assertEquals(
                "redirect:/login",
                controller.leaveGameSession(
                        "ABC234", () -> "anonymousUser", new RedirectAttributesModelMap())
        );
        verifyNoInteractions(leaveService);
    }

    @Test
    void shouldShowErrorMessageWhenUserDoesNotBelongToSession() {
        final LeaveGameSessionService leaveService = mock(LeaveGameSessionService.class);
        final GameSessionController controller = new GameSessionController(
                mock(CreateGameSessionService.class),
                mock(GameStateService.class),
                leaveService
        );
        final RedirectAttributes attributes = new RedirectAttributesModelMap();
        doThrow(new SecurityException("Δεν συμμετέχεις σε αυτή τη συνεδρία."))
                .when(leaveService)
                .leaveSession("ABC234", "intruder");

        assertEquals(
                "redirect:/multiplayer-room",
                controller.leaveGameSession("ABC234", () -> "intruder", attributes)
        );
        assertEquals(
                "Δεν συμμετέχεις σε αυτή τη συνεδρία.",
                attributes.getFlashAttributes().get("leaveSessionErrorMessage")
        );
    }
}