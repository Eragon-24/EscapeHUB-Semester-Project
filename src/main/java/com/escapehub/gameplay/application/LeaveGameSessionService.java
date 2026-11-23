package com.escapehub.gameplay.application;

import com.escapehub.gameplay.domain.GameSession;
import com.escapehub.gameplay.domain.SessionParticipants;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * US-014: Removes the authenticated participant from an active game session.
 */
@Service
public class LeaveGameSessionService {

    private final GameSessionRepository gameSessionRepository;
    private final SessionParticipantsRepository participantsRepository;
    private final SessionPlayerPositionsRepository positionsRepository;

    public LeaveGameSessionService(
            final GameSessionRepository gameSessionRepository,
            final SessionParticipantsRepository participantsRepository,
            final SessionPlayerPositionsRepository positionsRepository) {
        this.gameSessionRepository = gameSessionRepository;
        this.participantsRepository = participantsRepository;
        this.positionsRepository = positionsRepository;
    }

    /**
     * Removes the authenticated participant without changing the session or shared game state.
     *
     * @param joinCode session join code
     * @param username authenticated participant's username
     */
    public synchronized void leaveSession(final String joinCode, final String username) {
        if (joinCode == null || joinCode.isBlank()) {
            throw new IllegalArgumentException("Ο κωδικός συνεδρίας δεν μπορεί να είναι κενός.");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Απαιτείται σύνδεση χρήστη.");
        }

        final String normalizedCode = joinCode.trim().toUpperCase(Locale.ROOT);
        final GameSession session = gameSessionRepository.findByJoinCode(normalizedCode)
                .orElseThrow(() -> new GameSessionNotFoundException(
                        "Μη έγκυρος κωδικός συνεδρίας."));
        final SessionParticipants participants = participantsRepository
                .findBySessionId(session.id())
                .orElseGet(() -> new SessionParticipants(session.id(), session.creatorUsername()));

        if (!participants.isActive()) {
            throw new IllegalStateException("Η συνεδρία δεν είναι πλέον ενεργή.");
        }

        final String cleanUsername = username.trim();
        final Optional<String> participant = participants.getPlayers().stream()
                .filter(player -> player.equalsIgnoreCase(cleanUsername))
                .findFirst();
        if (participant.isEmpty()) {
            throw new SecurityException("Δεν συμμετέχεις σε αυτή τη συνεδρία.");
        }

        participants.removePlayer(participant.get());
        participantsRepository.save(participants);
        positionsRepository.findBySessionId(session.id()).ifPresent(tracked -> {
            tracked.removePlayer(participant.get());
            positionsRepository.save(tracked);
        });
    }
}
