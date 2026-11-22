package com.escapehub.gameplay.application;

import com.escapehub.gameplay.domain.Game;
import com.escapehub.gameplay.domain.GameSession;
import com.escapehub.gameplay.domain.GameState;
import com.escapehub.gameplay.domain.SessionParticipants;
import com.escapehub.gameplay.domain.SharedStateUpdate;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * US-015: delivers changes of the shared game state (the {@link Game} of US-013) to the other participants of the
 * same game session in real time.
 *
 * <p>The shared state itself is the {@code Game} of the session; this service does not keep a model of its own. A
 * change requested by a client is accepted only if the session exists and is active, the requesting user takes part
 * in it, the request is well-formed and no {@link StateChangeRule} objects. Only then is it applied to the
 * {@code Game} (as an attribute) and published to the participants of that session. The server stays
 * authoritative: clients never decide the official state themselves.</p>
 */
@Service
public class SharedStateService {

    /** Allowed state keys. */
    public static final Pattern KEY_PATTERN = Pattern.compile("[A-Za-z0-9._:-]{1,64}");

    /** Longest allowed value. */
    public static final int MAX_VALUE_LENGTH = 1000;

    private final GameSessionRepository sessions;
    private final SessionParticipantsRepository participants;
    private final GameStateService gameStates;
    private final GameRepository games;
    private final SessionUpdateChannel channel;
    private final List<StateChangeRule> rules;
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param sessions session metadata
     * @param participants session participants
     * @param gameStates the US-013 service that initializes and loads the game of a session
     * @param games storage of the games (US-013)
     * @param channel real-time delivery channel
     * @param rules rules contributed by the gameplay stories (may be empty)
     * @param clock the clock
     */
    public SharedStateService(
            final GameSessionRepository sessions,
            final SessionParticipantsRepository participants,
            final GameStateService gameStates,
            final GameRepository games,
            final SessionUpdateChannel channel,
            final List<StateChangeRule> rules,
            final Clock clock) {
        this.sessions = sessions;
        this.participants = participants;
        this.gameStates = gameStates;
        this.games = games;
        this.channel = channel;
        this.rules = List.copyOf(rules);
        this.clock = clock;
    }

    /**
     * Checks that the user may take part in the real-time updates of a session.
     *
     * @param sessionId the session
     * @param username the user
     * @throws NoSuchElementException when the session does not exist
     * @throws AccessDeniedException when the user is not a participant of the session
     */
    public void requireParticipant(final UUID sessionId, final String username) {
        final GameSession session = sessions.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Η συνεδρία δεν βρέθηκε."));
        final String user = username == null ? "" : username.trim();
        final boolean creator = session.creatorUsername().equalsIgnoreCase(user);
        final boolean joined = participants.findBySessionId(sessionId)
                .map(p -> p.getPlayers().stream().anyMatch(player -> player.equalsIgnoreCase(user)))
                .orElse(false);
        if (user.isEmpty() || !(creator || joined)) {
            throw new AccessDeniedException("Δεν συμμετέχεις σε αυτή τη συνεδρία.");
        }
    }

    /**
     * Returns the current official game state of a session. The game of the session is initialized on first access
     * (US-013), so a participant who connects before anything happened receives the initial state.
     *
     * @param sessionId the session
     * @param username the requesting participant
     * @return the state of the session's game
     */
    public GameState snapshot(final UUID sessionId, final String username) {
        requireParticipant(sessionId, username);
        return gameOf(sessionId, username).snapshot();
    }

    /**
     * Validates, applies and publishes a state change requested by a participant.
     *
     * @param sessionId the session
     * @param username the requesting participant
     * @param key the state entry (stored as an attribute of the game)
     * @param value the requested value
     * @return the accepted update
     * @throws NoSuchElementException when the session does not exist
     * @throws AccessDeniedException when the user is not a participant
     * @throws StateChangeRejectedException when the request is invalid or a rule refuses it
     * @throws IllegalStateException when the session is no longer active
     */
    public SharedStateUpdate applyChange(
            final UUID sessionId, final String username, final String key, final String value) {
        requireParticipant(sessionId, username);
        final boolean active = participants.findBySessionId(sessionId).map(SessionParticipants::isActive).orElse(true);
        if (!active) {
            throw new IllegalStateException("Η συνεδρία δεν είναι πλέον ενεργή.");
        }
        if (key == null || !KEY_PATTERN.matcher(key).matches()) {
            throw new StateChangeRejectedException("Μη έγκυρο κλειδί κατάστασης.");
        }
        if (value == null || value.length() > MAX_VALUE_LENGTH) {
            throw new StateChangeRejectedException("Μη έγκυρη τιμή κατάστασης.");
        }
        final String author = username.trim();
        for (final StateChangeRule rule : rules) {
            rule.check(sessionId, author, key, value);
        }
        final Game game = gameOf(sessionId, author);
        synchronized (game) {
            game.putAttribute(key, value);
            final SharedStateUpdate update = new SharedStateUpdate(
                    sessionId, game.snapshot().version(), key, value, author, Instant.now(clock));
            channel.publish(update);
            return update;
        }
    }

    /**
     * Loads the game of the session, initializing it through the US-013 service when it does not exist yet.
     */
    private Game gameOf(final UUID sessionId, final String username) {
        final GameSession session = sessions.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Η συνεδρία δεν βρέθηκε."));
        try {
            gameStates.enterGame(session.joinCode(), username.trim());
        } catch (SecurityException e) {
            throw new AccessDeniedException(e.getMessage());
        }
        return games.findBySessionId(sessionId)
                .orElseThrow(() -> new IllegalStateException("Το παιχνίδι δεν έχει ξεκινήσει."));
    }
}
