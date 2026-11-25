package com.escapehub.gameplay.application;

import com.escapehub.gameplay.domain.GameSession;
import com.escapehub.gameplay.domain.GameState;
import com.escapehub.gameplay.domain.PuzzleSubmissionResult;
import com.escapehub.gameplay.domain.SharedStateUpdate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * US-015: validates requested shared-state changes and delegates every mutation to the US-013
 * GameStateService, which publishes the resulting update to the session's subscribers.
 */
@Service
public class SharedStateService {

    /** Allowed state keys. */
    public static final Pattern KEY_PATTERN = Pattern.compile("[A-Za-z0-9._:-]{1,64}");

    /** Longest allowed value. */
    public static final int MAX_VALUE_LENGTH = 1000;

    private static final Set<String> RESERVED_KEYS = Set.of(
            "game.currentRoomId",
            "game.status",
            "game.exitUnlocked");

    private final GameSessionRepository sessions;
    private final SessionParticipantsRepository participants;
    private final GameStateService gameStates;
    private final List<StateChangeRule> rules;

    /**
     * Creates the service.
     *
     * @param sessions session metadata
     * @param participants session participants
     * @param gameStates the US-013 game state service
     * @param rules rules contributed by gameplay stories; may be empty
     */
    public SharedStateService(
            final GameSessionRepository sessions,
            final SessionParticipantsRepository participants,
            final GameStateService gameStates,
            final List<StateChangeRule> rules) {
        this.sessions = sessions;
        this.participants = participants;
        this.gameStates = gameStates;
        this.rules = List.copyOf(rules);
    }

    /**
     * Checks that the user belongs to an existing, active session.
     *
     * @param sessionId the session
     * @param username the user
     * @throws NoSuchElementException when the session does not exist
     * @throws AccessDeniedException when the user is not a participant
     * @throws IllegalStateException when the session is inactive
     */
    public void requireParticipant(final UUID sessionId, final String username) {
        final GameSession session = sessions.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Η συνεδρία δεν βρέθηκε."));
        final String user = username == null ? "" : username.trim();
        final var tracked = participants.findBySessionId(sessionId);
        final boolean joined = tracked
                .map(p -> p.getPlayers().stream().anyMatch(player -> player.equalsIgnoreCase(user)))
                .orElse(session.creatorUsername().equalsIgnoreCase(user));
        if (user.isEmpty() || !joined) {
            throw new AccessDeniedException("Δεν συμμετέχεις σε αυτή τη συνεδρία.");
        }
        if (tracked.map(p -> !p.isActive()).orElse(false)) {
            throw new IllegalStateException("Η συνεδρία δεν είναι πλέον ενεργή.");
        }
    }

    /**
     * Returns the current official game state. The game is initialized on first access.
     *
     * @param sessionId the session
     * @param username the requesting participant
     * @return the current game state
     */
    public GameState snapshot(final UUID sessionId, final String username) {
        final GameSession session = sessions.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Η συνεδρία δεν βρέθηκε."));
        requireParticipant(sessionId, username);
        try {
            return gameStates.enterGame(session.joinCode(), username.trim());
        } catch (SecurityException e) {
            throw new AccessDeniedException(e.getMessage());
        }
    }

    /**
     * Validates and applies a participant's requested attribute change, then publishes the accepted event.
     *
     * @param sessionId the session
     * @param username the requesting participant
     * @param key the attribute name
     * @param value the requested value
     * @return the exact event sent to subscribers
     */
    public SharedStateUpdate applyChange(
            final UUID sessionId, final String username, final String key, final String value) {
        requireParticipant(sessionId, username);
        if (key == null || !KEY_PATTERN.matcher(key).matches()
                || RESERVED_KEYS.contains(key) || key.startsWith("progress.")) {
            throw new StateChangeRejectedException("Μη έγκυρο κλειδί κατάστασης.");
        }
        if (value == null || value.length() > MAX_VALUE_LENGTH) {
            throw new StateChangeRejectedException("Μη έγκυρη τιμή κατάστασης.");
        }
        final String author = username.trim();
        for (final StateChangeRule rule : rules) {
            rule.check(sessionId, author, key, value);
        }
        final GameSession session = sessions.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Η συνεδρία δεν βρέθηκε."));
        try {
            // The session's Game is created lazily by US-013. Ensure it exists before applying the first update.
            gameStates.enterGame(session.joinCode(), author);
            return gameStates.updateAttributeAndPublish(session.joinCode(), author, key, value);
        } catch (SecurityException e) {
            throw new AccessDeniedException(e.getMessage());
        }
    }

    /**
     * Validates and submits a participant's answer against a puzzle in their session.
     *
     * @param sessionId session identifier
     * @param username authenticated participant
     * @param puzzleId puzzle identifier
     * @param answer submitted answer
     * @return result and updated session state
     */
    public PuzzleSubmissionResult submitPuzzleAnswer(
            final UUID sessionId,
            final String username,
            final String puzzleId,
            final String answer) {
        requireParticipant(sessionId, username);
        final GameSession session = sessions.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Η συνεδρία δεν βρέθηκε."));
        try {
            gameStates.enterGame(session.joinCode(), username.trim());
            return gameStates.submitPuzzleAnswer(session.joinCode(), username.trim(), puzzleId, answer);
        } catch (SecurityException e) {
            throw new AccessDeniedException(e.getMessage());
        }
    }
}
