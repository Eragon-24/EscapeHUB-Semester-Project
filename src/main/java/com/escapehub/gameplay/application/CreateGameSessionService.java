package com.escapehub.gameplay.application;

import com.escapehub.gameplay.domain.GameSession;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * US-011: creates a game session and assigns it a unique join code.
 *
 * <p>This use case creates session metadata only. Joining, participant presence,
 * shared gameplay state and persistence are separate extension points.</p>
 */
@Service
public class CreateGameSessionService {

    private static final int JOIN_CODE_LENGTH = 6;
    private static final int MAX_CODE_ATTEMPTS = 100;
    private static final String JOIN_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final GameSessionRepository gameSessionRepository;
    private final SecureRandom random = new SecureRandom();

    public CreateGameSessionService(final GameSessionRepository gameSessionRepository) {
        this.gameSessionRepository = gameSessionRepository;
    }

    /**
     * Creates a session owned by the currently authenticated username.
     *
     * @param creatorUsername authenticated creator's username
     * @return newly stored session metadata
     */
    public synchronized GameSession createSession(final String creatorUsername) {
        if (creatorUsername == null || creatorUsername.isBlank()) {
            throw new IllegalArgumentException("Creator username must not be blank");
        }

        final String cleanUsername = creatorUsername.trim();
        final String joinCode = generateUniqueJoinCode();
        final GameSession session = new GameSession(
                UUID.randomUUID(),
                joinCode,
                cleanUsername,
                Instant.now()
        );
        return gameSessionRepository.save(session);
    }

    private String generateUniqueJoinCode() {
        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            final StringBuilder code = new StringBuilder(JOIN_CODE_LENGTH);
            for (int index = 0; index < JOIN_CODE_LENGTH; index++) {
                final int characterIndex = random.nextInt(JOIN_CODE_ALPHABET.length());
                code.append(JOIN_CODE_ALPHABET.charAt(characterIndex));
            }
            final String candidate = code.toString();
            if (!gameSessionRepository.existsByJoinCode(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not generate a unique game-session code");
    }
}
