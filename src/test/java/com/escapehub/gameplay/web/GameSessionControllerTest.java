package com.escapehub.gameplay.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.escapehub.gameplay.application.CreateGameSessionService;
import com.escapehub.gameplay.domain.GameSession;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

class GameSessionControllerTest {

    @Test
    void shouldCreateSessionForAuthenticatedPrincipalAndShowInviteCode() {
        final CreateGameSessionService service = mock(CreateGameSessionService.class);
        final GameSessionController controller = new GameSessionController(service);
        final GameSession session = new GameSession(
                UUID.randomUUID(),
                "ABC234",
                "playerOne",
                Instant.parse("2026-11-19T00:00:00Z")
        );
        final RedirectAttributes attributes = new RedirectAttributesModelMap();
        when(service.createSession("playerOne")).thenReturn(session);

        final String result = controller.createGameSession(() -> "playerOne", attributes);

        assertEquals("redirect:/multiplayer-room", result);
        assertEquals("ABC234", attributes.getFlashAttributes().get("createdSessionCode"));
        assertEquals(session.id().toString(), attributes.getFlashAttributes().get("createdSessionId"));
        verify(service).createSession("playerOne");
    }

    @Test
    void shouldRedirectAnonymousVisitorToLoginWithoutCreatingSession() {
        final CreateGameSessionService service = mock(CreateGameSessionService.class);
        final GameSessionController controller = new GameSessionController(service);

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
        final GameSessionController controller = new GameSessionController(service);

        assertEquals(
                "redirect:/login",
                controller.createGameSession(null, new RedirectAttributesModelMap())
        );
        verifyNoInteractions(service);
    }
}
