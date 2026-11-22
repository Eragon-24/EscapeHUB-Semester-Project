package com.escapehub.gameplay.application;

import com.escapehub.gameplay.domain.Game;
import com.escapehub.gameplay.domain.GameSession;
import com.escapehub.gameplay.domain.GameState;
import com.escapehub.gameplay.domain.SessionParticipants;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class GameStateService {

    private final GameSessionRepository gameSessionRepository;
    private final SessionParticipantsRepository participantsRepository;
    private final GameRepository gameRepository;
    public GameStateService(
            final GameSessionRepository gameSessionRepository,
            final SessionParticipantsRepository participantsRepository,
            final GameRepository gameRepository) {
        this.gameSessionRepository = gameSessionRepository;
        this.participantsRepository = participantsRepository;
        this.gameRepository = gameRepository;
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
        game.changeRoom(roomId);
        return game.snapshot();
    }
    public synchronized GameState updateAttribute(
            final String joinCode, final String username, final String key, final String value) {
        final Game game = requireGame(joinCode, username);
        game.putAttribute(key, value);
        return game.snapshot();
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
                .orElseThrow(() -> new IllegalArgumentException("Μη έγκυρος κωδικός συνεδρίας."));
    }

    private void requireParticipant(final GameSession session, final String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Απαιτείται σύνδεση χρήστη.");
        }
        final String name = username.trim();
        final Optional<SessionParticipants> participants =
                participantsRepository.findBySessionId(session.id());
        final boolean isMember = participants
                .map(p -> p.getPlayers().contains(name))
                .orElse(session.creatorUsername().equals(name));
        if (!isMember) {
            throw new SecurityException("Δεν συμμετέχεις σε αυτή τη συνεδρία.");
        }
        if (participants.map(p -> !p.isActive()).orElse(false)) {
            throw new IllegalStateException("Η συνεδρία δεν είναι πλέον ενεργή.");
        }
    }
}