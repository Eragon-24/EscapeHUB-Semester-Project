package com.escapehub.gameplay.web;

import com.escapehub.gameplay.application.GameSessionRepository;
import com.escapehub.gameplay.application.SessionParticipantsRepository;
import com.escapehub.gameplay.domain.GameSession;
import com.escapehub.gameplay.domain.SessionParticipants;
import com.escapehub.gameplay.domain.Room;
import java.security.Principal;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
public class MultiplayerRoomController {

    private static final String JOIN_CODE_PATTERN = "[A-Za-z0-9]{6}";

    private final GameSessionRepository gameSessionRepository;
    private final SessionParticipantsRepository participantsRepository;
    private final Room room = new Room();

    public MultiplayerRoomController(
            final GameSessionRepository gameSessionRepository,
            final SessionParticipantsRepository participantsRepository) {
        this.gameSessionRepository = gameSessionRepository;
        this.participantsRepository = participantsRepository;
    }

    @GetMapping("/multiplayer-room")
    public String showRoom(
            @RequestParam(name = "session", required = false) final String session,
            @RequestParam(name = "sessionId", required = false) final String sessionId,
            final Principal principal,
            final Model model) {
        if (session != null && session.trim().matches(JOIN_CODE_PATTERN)
                && isAuthenticated(principal)) {
            final String normalizedCode = session.trim().toUpperCase(Locale.ROOT);
            model.addAttribute("activeSessionCode", normalizedCode);
            model.addAttribute("canLeaveSession", isActiveParticipant(normalizedCode, principal.getName()));
        }
        if (sessionId != null && !sessionId.isBlank() && isAuthenticated(principal)) {
            model.addAttribute("activeSessionId", sessionId.trim());
        }
        model.addAttribute("roomName", room.getName());
        model.addAttribute("roomDescription", room.getDescription());
        model.addAttribute("canCreateSession", isAuthenticated(principal));
        if (!model.containsAttribute("canLeaveSession")) {
            model.addAttribute("canLeaveSession", false);
        }
        return "multiplayer-room";
    }

    private boolean isActiveParticipant(final String joinCode, final String username) {
        final Optional<GameSession> session = gameSessionRepository.findByJoinCode(joinCode);
        if (session.isEmpty()) {
            return false;
        }

        final GameSession gameSession = session.get();
        final Optional<SessionParticipants> participants = participantsRepository
                .findBySessionId(gameSession.id());
        if (participants.isEmpty()) {
            return gameSession.creatorUsername().equalsIgnoreCase(username);
        }

        final SessionParticipants sessionParticipants = participants.get();
        return sessionParticipants.isActive()
                && sessionParticipants.getPlayers().stream()
                .anyMatch(player -> player.equalsIgnoreCase(username));
    }

    private boolean isAuthenticated(final Principal principal) {
        return principal != null && !"anonymousUser".equals(principal.getName());
    }
}
