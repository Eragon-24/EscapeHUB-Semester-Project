package com.escapehub.gameplay.application;

import com.escapehub.gameplay.domain.GameSession;
import com.escapehub.gameplay.domain.SessionParticipants;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * US-012: Allows an authenticated user to join an active game session by code.
 */
@Service
public class JoinGameSessionService {

    private final GameSessionRepository gameSessionRepository;
    private final SessionParticipantsRepository participantsRepository;

    /**
     * Creates the join-session use case service.
     *
     * @param gameSessionRepository repository for session metadata
     * @param participantsRepository repository for session participants
     */
    public JoinGameSessionService(
            final GameSessionRepository gameSessionRepository,
            final SessionParticipantsRepository participantsRepository) {
        this.gameSessionRepository = gameSessionRepository;
        this.participantsRepository = participantsRepository;
    }

    /**
     * Joins an active session using its 6-character join code.
     *
     * @param joinCode invite code entered by the player
     * @param username authenticated player's username
     * @return updated session participants
     */
    public synchronized SessionParticipants joinSession(
            final String joinCode,
            final String username) {
        if (joinCode == null || joinCode.isBlank()) {
            throw new IllegalArgumentException("Ο κωδικός συνεδρίας δεν μπορεί να είναι κενός.");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Απαιτείται σύνδεση χρήστη.");
        }

        final String normalizedCode = joinCode.trim().toUpperCase(Locale.ROOT);
        final GameSession session = gameSessionRepository.findByJoinCode(normalizedCode)
                .orElseThrow(() -> new IllegalArgumentException("Μη έγκυρος κωδικός συνεδρίας."));

        final SessionParticipants participants = participantsRepository
                .findBySessionId(session.id())
                .orElseGet(() -> new SessionParticipants(session.id(), session.creatorUsername()));

        participants.addPlayer(username);
        return participantsRepository.save(participants);
    }
}