package com.escapehub.gameplay.web;

import com.escapehub.gameplay.application.CreateGameSessionService;
import com.escapehub.gameplay.application.GameStateService;
import com.escapehub.gameplay.domain.GameSession;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class GameSessionController {

    private final CreateGameSessionService createGameSessionService;
    private final GameStateService gameStateService;

    public GameSessionController(
            final CreateGameSessionService createGameSessionService,
            final GameStateService gameStateService) {
        this.createGameSessionService = createGameSessionService;
        this.gameStateService = gameStateService;
    }

    @PostMapping("/game-sessions")
    public String createGameSession(
            final Principal principal,
            final RedirectAttributes redirectAttributes) {
        if (!isAuthenticated(principal)) {
            return "redirect:/login";
        }

        final GameSession session = createGameSessionService.createSession(principal.getName());
        redirectAttributes.addFlashAttribute("createdSessionCode", session.joinCode());
        redirectAttributes.addFlashAttribute("createdSessionId", session.id().toString());
        gameStateService.enterGame(session.joinCode(), principal.getName());
        return "redirect:/multiplayer-room?session=" + session.joinCode();
    }

    private boolean isAuthenticated(final Principal principal) {
        return principal != null && !"anonymousUser".equals(principal.getName());
    }
}