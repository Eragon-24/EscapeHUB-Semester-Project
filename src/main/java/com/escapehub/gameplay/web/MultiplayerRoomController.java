package com.escapehub.gameplay.web;

import com.escapehub.gameplay.domain.Room;
import java.security.Principal;
import java.util.Locale;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
public class MultiplayerRoomController {

    private static final String JOIN_CODE_PATTERN = "[A-Za-z0-9]{6}";

    private final Room room = new Room();

    @GetMapping("/multiplayer-room")
    public String showRoom(
            @RequestParam(name = "session", required = false) final String session,
            final Principal principal,
            final Model model) {
        if (session != null && session.trim().matches(JOIN_CODE_PATTERN)
                && isAuthenticated(principal)) {
            model.addAttribute("activeSessionCode", session.trim().toUpperCase(Locale.ROOT));
        }
        model.addAttribute("roomName", room.getName());
        model.addAttribute("roomDescription", room.getDescription());
        model.addAttribute("canCreateSession", isAuthenticated(principal));
        return "multiplayer-room";
    }

    private boolean isAuthenticated(final Principal principal) {
        return principal != null && !"anonymousUser".equals(principal.getName());
    }
}