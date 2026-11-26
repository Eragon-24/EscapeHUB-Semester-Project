package com.escapehub.gameplay.application;

import com.escapehub.gameplay.domain.Game;
import com.escapehub.gameplay.domain.GameSession;
import com.escapehub.gameplay.domain.GameState;
import com.escapehub.gameplay.domain.SessionParticipants;
import com.escapehub.gameplay.domain.SharedStateUpdate;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class GameStateService {

    private static final String CURRENT_ROOM_KEY = "game.currentRoomId";

    private final GameSessionRepository gameSessionRepository;
    private final SessionParticipantsRepository participantsRepository;
    private final GameRepository gameRepository;
    private final SessionUpdateChannel updateChannel;
    private final Clock clock;

    public GameStateService(
            final GameSessionRepository gameSessionRepository,
            final SessionParticipantsRepository participantsRepository,
            final GameRepository gameRepository,
            final SessionUpdateChannel updateChannel,
            final Clock clock) {
        this.gameSessionRepository = gameSessionRepository;
        this.participantsRepository = participantsRepository;
        this.gameRepository = gameRepository;
        this.updateChannel = updateChannel;
        this.clock = clock;
    }

    public synchronized GameState enterGame(final String joinCode, final String username) {
        final GameSession session = findSession(joinCode);
        requireParticipant(session, username);
        final Game game = gameRepository.findBySessionId(session.id())
                .orElseGet(() -> gameRepository.save(new Game(session.id())));
        return game.snapshot();
    }

    public synchronized Optional<GameState> getState(final String joinCode, final String username) {
        final GameSession session = findSession(joinCode);
        requireParticipant(session, username);
        return gameRepository.findBySessionId(session.id()).map(Game::snapshot);
    }

    public synchronized GameState changeRoom(
            final String joinCode, final String username, final String roomId) {
        final Game game = requireGame(joinCode, username);
        synchronized (game) {
            game.changeRoom(roomId);
            gameRepository.save(game);
            final GameState snapshot = game.snapshot();
            publishUpdate(snapshot, username, CURRENT_ROOM_KEY, snapshot.currentRoomId());
            return snapshot;
        }
    }

    public synchronized GameState updateAttribute(
            final String joinCode, final String username, final String key, final String value) {
        return mutateAttribute(joinCode, username, key, value).state();
    }

    /**
     * Applies an attribute change and returns the exact event published to the session subscribers.
     *
     * @param joinCode session join code
     * @param username authenticated participant
     * @param key attribute name
     * @param value new attribute value
     * @return the published event
     */
    public synchronized SharedStateUpdate updateAttributeAndPublish(
            final String joinCode, final String username, final String key, final String value) {
        return mutateAttribute(joinCode, username, key, value).update();
    }

    /**
     * US-022: sets an attribute only if it has no value yet, then persists and publishes it. The check and the
     * change happen as one step (all mutations go through the synchronized methods of this service), so of several
     * simultaneous calls exactly one succeeds. Used for effects that must happen only once.
     *
     * @param joinCode session join code
     * @param username authenticated participant
     * @param key attribute name
     * @param value attribute value
     * @return the published event, or empty if the attribute already existed (nothing changed or was published)
     */
    public synchronized Optional<SharedStateUpdate> setAttributeIfAbsent(
            final String joinCode, final String username, final String key, final String value) {
        if (requireGame(joinCode, username).snapshot().attributes().containsKey(key)) {
            return Optional.empty();
        }
        return Optional.of(mutateAttribute(joinCode, username, key, value).update());
    }

    private MutationResult mutateAttribute(
            final String joinCode, final String username, final String key, final String value) {
        final Game game = requireGame(joinCode, username);
        synchronized (game) {
            game.putAttribute(key, value);
            gameRepository.save(game);
            final GameState snapshot = game.snapshot();
            final SharedStateUpdate update = publishUpdate(snapshot, username, key.trim(), value);
            return new MutationResult(snapshot, update);
        }
    }

    private SharedStateUpdate publishUpdate(
            final GameState snapshot, final String username, final String key, final String value) {
        final SharedStateUpdate update = new SharedStateUpdate(
                snapshot.sessionId(),
                snapshot.version(),
                key,
                value,
                username.trim(),
                Instant.now(clock));
        updateChannel.publish(update);
        return update;
    }

    private Game requireGame(final String joinCode, final String username) {
        final GameSession session = findSession(joinCode);
        requireParticipant(session, username);
        return gameRepository.findBySessionId(session.id())
                .orElseThrow(() -> new IllegalStateException(
                        "Το παιχνίδι δεν έχει ξεκινήσει για αυτή τη συνεδρία."));
    }

    private GameSession findSession(final String joinCode) {
        if (joinCode == null || joinCode.isBlank()) {
            throw new IllegalArgumentException("Ο κωδικός συνεδρίας δεν μπορεί να είναι κενός.");
        }
        return gameSessionRepository.findByJoinCode(joinCode.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new GameSessionNotFoundException("Μη έγκυρος κωδικός συνεδρίας."));
    }

    private void requireParticipant(final GameSession session, final String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Απαιτείται σύνδεση χρήστη.");
        }
        final String name = username.trim();
        final Optional<SessionParticipants> participants =
                participantsRepository.findBySessionId(session.id());
        final boolean isMember = participants
                .map(p -> p.getPlayers().stream().anyMatch(player -> player.equalsIgnoreCase(name)))
                .orElse(session.creatorUsername().equalsIgnoreCase(name));
        if (!isMember) {
            throw new SecurityException("Δεν συμμετέχεις σε αυτή τη συνεδρία.");
        }
        if (participants.map(p -> !p.isActive()).orElse(false)) {
            throw new IllegalStateException("Η συνεδρία δεν είναι πλέον ενεργή.");
        }
    }

    private record MutationResult(GameState state, SharedStateUpdate update) {
    }
}
