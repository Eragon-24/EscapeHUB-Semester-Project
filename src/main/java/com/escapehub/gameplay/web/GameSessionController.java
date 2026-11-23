package com.escapehub.gameplay.web;

import com.escapehub.gameplay.application.CreateGameSessionService;
import com.escapehub.gameplay.application.GameSessionNotFoundException;
import com.escapehub.gameplay.application.GameStateService;
import com.escapehub.gameplay.application.LeaveGameSessionService;
import com.escapehub.gameplay.domain.GameSession;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class GameSessionController {

    private final CreateGameSessionService createGameSessionService;
    private final GameStateService gameStateService;
    private final LeaveGameSessionService leaveGameSessionService;

    public GameSessionController(
            final CreateGameSessionService createGameSessionService,
            final GameStateService gameStateService,
            final LeaveGameSessionService leaveGameSessionService) {
        this.createGameSessionService = createGameSessionService;
        this.gameStateService = gameStateService;
        this.leaveGameSessionService = leaveGameSessionService;
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

    @PostMapping("/game-sessions/{joinCode}/leave")
    public String leaveGameSession(
            @PathVariable("joinCode") final String joinCode,
            final Principal principal,
            final RedirectAttributes redirectAttributes) {
        if (!isAuthenticated(principal)) {
            return "redirect:/login";
        }

        try {
            leaveGameSessionService.leaveSession(joinCode, principal.getName());
            redirectAttributes.addFlashAttribute(
                    "leaveSessionMessage",
                    "Αποχώρησες από τη συνεδρία. Το παιχνίδι συνεχίζεται για τους υπόλοιπους."
            );
            redirectAttributes.addFlashAttribute("sessionLeft", true);
        } catch (final IllegalArgumentException | IllegalStateException | SecurityException
                       | GameSessionNotFoundException exception) {
            redirectAttributes.addFlashAttribute("leaveSessionErrorMessage", exception.getMessage());
        }
        return "redirect:/multiplayer-room";
    }

    private boolean isAuthenticated(final Principal principal) {
        return principal != null && !"anonymousUser".equals(principal.getName());
    }
}