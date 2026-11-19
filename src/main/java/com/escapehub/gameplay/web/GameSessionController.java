package com.escapehub.gameplay.web;

import com.escapehub.gameplay.application.CreateGameSessionService;
import com.escapehub.gameplay.domain.GameSession;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Web entry point for US-011 session creation.
 *
 * <p>The creator identity comes from the authenticated request, never from a
 * user-supplied form field.</p>
 */
@Controller
public class GameSessionController {

    private final CreateGameSessionService createGameSessionService;

    public GameSessionController(final CreateGameSessionService createGameSessionService) {
        this.createGameSessionService = createGameSessionService;
    }

    /**
     * Creates a session and displays its invite code on the room page.
     *
     * @param principal authenticated user
     * @param redirectAttributes temporary values for the redirected page
     * @return redirect to the multiplayer room view
     */
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
        return "redirect:/multiplayer-room";
    }

    private boolean isAuthenticated(final Principal principal) {
        return principal != null && !"anonymousUser".equals(principal.getName());
    }
}
