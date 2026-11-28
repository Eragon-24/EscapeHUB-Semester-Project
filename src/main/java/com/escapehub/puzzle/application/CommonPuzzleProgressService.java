package com.escapehub.puzzle.application;

import com.escapehub.gameplay.application.GameRepository;
import com.escapehub.gameplay.application.GameSessionNotFoundException;
import com.escapehub.gameplay.application.GameSessionRepository;
import com.escapehub.gameplay.application.SessionParticipantsRepository;
import com.escapehub.gameplay.domain.Game;
import com.escapehub.gameplay.domain.GameSession;
import com.escapehub.gameplay.domain.GameState;
import com.escapehub.gameplay.domain.SessionParticipants;
import com.escapehub.puzzle.domain.CommonPuzzleProgressView;
import com.escapehub.puzzle.domain.PuzzleDefinition;
import com.escapehub.puzzle.domain.PuzzleProgressItem;
import com.escapehub.puzzle.domain.PuzzleStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Reads the authoritative shared GameState and exposes common puzzle progress. */
@Service
public class CommonPuzzleProgressService {
    private final GameRepository games;
    private final GameSessionRepository sessions;
    private final SessionParticipantsRepository participants;
    private final PuzzleCatalog puzzles;

    public CommonPuzzleProgressService(final GameRepository games, final GameSessionRepository sessions,
            final SessionParticipantsRepository participants, final PuzzleCatalog puzzles) {
        this.games = games;
        this.sessions = sessions;
        this.participants = participants;
        this.puzzles = puzzles;
    }

    public CommonPuzzleProgressView getCommonProgress(final UUID sessionId, final String username) {
        final GameSession session = sessions.findById(sessionId)
                .orElseThrow(() -> new GameSessionNotFoundException("Η συνεδρία δεν βρέθηκε."));
        requireActiveParticipant(session, username);

        final Optional<Game> game = games.findBySessionId(sessionId);
        final GameState snapshot = game.map(Game::snapshot).orElseGet(() -> new Game(sessionId).snapshot());
        final List<PuzzleDefinition> definitions = puzzles.findAll();
        final List<PuzzleProgressItem> items = definitions.stream()
                .map(puzzle -> {
                    final boolean solved = snapshot.completedPuzzleIds().contains(puzzle.id());
                    return new PuzzleProgressItem(
                            puzzle.id(),
                            puzzle.title(),
                            puzzle.prompt(),
                            solved ? PuzzleStatus.SOLVED : PuzzleStatus.UNSOLVED,
                            solved);
                })
                .toList();
        final int total = items.size();
        final int solved = (int) items.stream().filter(PuzzleProgressItem::solved).count();
        return new CommonPuzzleProgressView(sessionId, snapshot.currentRoomId(), total, solved,
                total - solved, total == 0 ? 0 : solved * 100 / total, snapshot.version(), items);
    }

    private void requireActiveParticipant(final GameSession session, final String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Απαιτείται σύνδεση χρήστη.");
        }
        final String clean = username.trim();
        final Optional<SessionParticipants> tracking = participants.findBySessionId(session.id());
        final boolean creator = session.creatorUsername().equalsIgnoreCase(clean);
        final boolean player = tracking.map(value -> value.getPlayers().stream()
                .anyMatch(name -> name.equalsIgnoreCase(clean))).orElse(false);
        if (!creator && !player) {
            throw new SecurityException("Δεν συμμετέχεις σε αυτή τη συνεδρία.");
        }
        if (tracking.isPresent() && !tracking.get().isActive()) {
            throw new IllegalStateException("Η συνεδρία δεν είναι πλέον ενεργή.");
        }
    }
}
