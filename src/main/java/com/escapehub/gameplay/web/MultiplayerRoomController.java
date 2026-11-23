package com.escapehub.gameplay.web;

import com.escapehub.gameplay.application.GameSessionRepository;
import com.escapehub.gameplay.application.SessionParticipantsRepository;
import com.escapehub.gameplay.domain.GameSession;
import com.escapehub.gameplay.domain.Room;
import com.escapehub.gameplay.domain.SessionParticipants;
import java.security.Principal;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
public class MultiplayerRoomController {

    private static final String JOIN_CODE_PATTERN = "[A-Za-z0-9]{6}";

    private final Room room = new Room();
    private final GameSessionRepository gameSessionRepository;
    private final SessionParticipantsRepository participantsRepository;

    public MultiplayerRoomController(
            final GameSessionRepository gameSessionRepository,
            final SessionParticipantsRepository participantsRepository) {
        this.gameSessionRepository = gameSessionRepository;
        this.participantsRepository = participantsRepository;
    }

    @GetMapping("/multiplayer-room")
    public String showRoom(
            @RequestParam(name = "session", required = false) final String session,
            final Principal principal,
            final Model model) {
        if (session != null && session.trim().matches(JOIN_CODE_PATTERN)
                && isAuthenticated(principal)) {
            final String normalizedCode = session.trim().toUpperCase(Locale.ROOT);
            model.addAttribute("activeSessionCode", normalizedCode);
            gameSessionRepository.findByJoinCode(normalizedCode).ifPresent(gameSession ->
                    addSessionMembership(gameSession, principal.getName(), model));
        }
        model.addAttribute("roomName", room.getName());
        model.addAttribute("roomDescription", room.getDescription());
        model.addAttribute("canCreateSession", isAuthenticated(principal));
        return "multiplayer-room";
    }

    private void addSessionMembership(
            final GameSession session, final String username, final Model model) {
        final Set<String> players = participantsRepository.findBySessionId(session.id())
                .map(SessionParticipants::getPlayers)
                .orElseGet(() -> Set.of(session.creatorUsername()));
        model.addAttribute("sessionPlayers", players);
        model.addAttribute("canLeaveSession", players.contains(username));
    }

    private boolean isAuthenticated(final Principal principal) {
        return principal != null && !"anonymousUser".equals(principal.getName());
    }
}
