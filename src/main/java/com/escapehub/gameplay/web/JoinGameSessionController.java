package com.escapehub.gameplay.web;

import com.escapehub.gameplay.application.GameSessionNotFoundException;
import com.escapehub.gameplay.application.GameStateService;
import com.escapehub.gameplay.application.JoinGameSessionService;
import com.escapehub.gameplay.domain.SessionParticipants;
import java.security.Principal;
import java.util.Locale;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
public class JoinGameSessionController {

    private final JoinGameSessionService joinGameSessionService;
    private final GameStateService gameStateService;


    public JoinGameSessionController(
            final JoinGameSessionService joinGameSessionService,
            final GameStateService gameStateService) {
        this.joinGameSessionService = joinGameSessionService;
        this.gameStateService = gameStateService;
    }


    @PostMapping("/game-sessions/join")
    public String joinGameSession(
            @RequestParam(name = "joinCode", required = false) final String joinCode,
            final Principal principal,
            final RedirectAttributes redirectAttributes) {
        if (!isAuthenticated(principal)) {
            return "redirect:/login";
        }
        try {
            final SessionParticipants participants = joinGameSessionService
                    .joinSession(joinCode, principal.getName());
            redirectAttributes.addFlashAttribute(
                    "joinedSessionCode",
                    joinCode.trim().toUpperCase(Locale.ROOT)
            );
            redirectAttributes.addFlashAttribute("sessionPlayers", participants.getPlayers());
            redirectAttributes.addFlashAttribute(
                    "gameState",
                    gameStateService.enterGame(joinCode, principal.getName()));
        } catch (final IllegalArgumentException | IllegalStateException
                       | GameSessionNotFoundException exception) {
            redirectAttributes.addFlashAttribute("joinErrorMessage", exception.getMessage());
        }
        return "redirect:/multiplayer-room";
    }

    private boolean isAuthenticated(final Principal principal) {
        return principal != null && !"anonymousUser".equals(principal.getName());
    }
}