package com.escapehub.gameplay.application;

import com.escapehub.gameplay.domain.GameSession;
import com.escapehub.gameplay.domain.SessionParticipants;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** US-014: removes the authenticated player from a multiplayer game session. */
@Service
public class LeaveGameSessionService {

    public enum LeaveStatus { LEFT, NOT_A_PARTICIPANT, SESSION_NOT_FOUND }

    public record LeaveResult(LeaveStatus status, String message) {
        public boolean left() {
            return status == LeaveStatus.LEFT;
        }
    }

    private final GameSessionRepository gameSessionRepository;
    private final SessionParticipantsRepository participantsRepository;
    private final OtherPlayersMovementService movementService;

    public LeaveGameSessionService(
            final GameSessionRepository gameSessionRepository,
            final SessionParticipantsRepository participantsRepository,
            final OtherPlayersMovementService movementService) {
        this.gameSessionRepository = gameSessionRepository;
        this.participantsRepository = participantsRepository;
        this.movementService = movementService;
    }

    public synchronized LeaveResult leaveSession(final String username, final String joinCode) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Απαιτείται σύνδεση χρήστη.");
        }
        if (joinCode == null || joinCode.isBlank()) {
            return new LeaveResult(LeaveStatus.SESSION_NOT_FOUND, "Η συνεδρία δεν βρέθηκε.");
        }

        final GameSession session = gameSessionRepository
                .findByJoinCode(joinCode.trim().toUpperCase(Locale.ROOT))
                .orElse(null);
        if (session == null) {
            return new LeaveResult(LeaveStatus.SESSION_NOT_FOUND, "Η συνεδρία δεν βρέθηκε.");
        }

        final String cleanUsername = username.trim();
        final Optional<SessionParticipants> existingParticipants =
                participantsRepository.findBySessionId(session.id());
        final SessionParticipants participants = existingParticipants.orElseGet(
                () -> new SessionParticipants(session.id(), session.creatorUsername()));
        if (!participants.getPlayers().contains(cleanUsername)) {
            return new LeaveResult(LeaveStatus.NOT_A_PARTICIPANT,
                    "Δεν συμμετέχεις σε αυτή τη συνεδρία.");
        }

        movementService.removePlayer(session.id(), cleanUsername);
        if (!participants.removePlayer(cleanUsername)) {
            return new LeaveResult(LeaveStatus.NOT_A_PARTICIPANT,
                    "Δεν συμμετέχεις σε αυτή τη συνεδρία.");
        }
        participantsRepository.save(participants);
        return new LeaveResult(LeaveStatus.LEFT, "Αποχώρησες από τη συνεδρία.");
    }
}
