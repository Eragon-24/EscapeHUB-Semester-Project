package com.escapehub.gameplay.web;

import com.escapehub.gameplay.application.LeaveGameSessionService;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Web endpoint for leaving the current multiplayer session (US-014). */
@Controller
public class LeaveGameSessionController {

    private final LeaveGameSessionService leaveGameSessionService;

    public LeaveGameSessionController(final LeaveGameSessionService leaveGameSessionService) {
        this.leaveGameSessionService = leaveGameSessionService;
    }

    @PostMapping("/game-sessions/{joinCode}/leave")
    public String leave(
            @PathVariable final String joinCode,
            final Principal principal,
            final RedirectAttributes redirectAttributes) {
        if (principal == null || "anonymousUser".equals(principal.getName())) {
            return "redirect:/login";
        }

        final LeaveGameSessionService.LeaveResult result =
                leaveGameSessionService.leaveSession(principal.getName(), joinCode);
        redirectAttributes.addFlashAttribute("leaveMessage", result.message());
        return "redirect:/multiplayer-room";
    }
}
