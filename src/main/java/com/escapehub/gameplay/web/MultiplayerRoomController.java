package com.escapehub.gameplay.web;

import com.escapehub.gameplay.domain.Room;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * US-014: serves the browser-based multiplayer room layout.
 *
 * <p>The view is intentionally presentation-only. Live player movement,
 * shared state and object actions are provided by their respective stories.</p>
 */
@Controller
public class MultiplayerRoomController {

    private final Room room = new Room();

    /**
     * Displays the room layout and the session-creation action when authenticated.
     *
     * @param principal current user, if authenticated
     * @param model values rendered by Thymeleaf
     * @return multiplayer room template
     */
    @GetMapping("/multiplayer-room")
    public String showRoom(final Principal principal, final Model model) {
        model.addAttribute("roomName", room.getName());
        model.addAttribute("roomDescription", room.getDescription());
        model.addAttribute("canCreateSession", isAuthenticated(principal));
        return "multiplayer-room";
    }

    private boolean isAuthenticated(final Principal principal) {
        return principal != null && !"anonymousUser".equals(principal.getName());
    }
}
