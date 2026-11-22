package com.escapehub.gameplay.web;

import com.escapehub.gameplay.application.JoinGameSessionService;
import com.escapehub.gameplay.domain.SessionParticipants;
import java.security.Principal;
import java.util.Locale;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Web entry point for US-012 joining a game session by code.
 */
@Controller
public class JoinGameSessionController {

    private final JoinGameSessionService joinGameSessionService;

    /**
     * Creates the join-session web controller.
     *
     * @param joinGameSessionService application service for joining sessions
     */
    public JoinGameSessionController(final JoinGameSessionService joinGameSessionService) {
        this.joinGameSessionService = joinGameSessionService;
    }

    /**
     * Joins an active session and displays participants or an error on the room page.
     *
     * @param joinCode session invite code submitted by the user
     * @param principal authenticated user
     * @param redirectAttributes flash attributes for the redirected view
     * @return redirect to the multiplayer room or login page
     */
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
            redirectAttributes.addFlashAttribute("joinedSessionId", participants.getSessionId());
        } catch (final IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("joinErrorMessage", exception.getMessage());
        }
        return "redirect:/multiplayer-room";
    }

    private boolean isAuthenticated(final Principal principal) {
        return principal != null && !"anonymousUser".equals(principal.getName());
    }
}