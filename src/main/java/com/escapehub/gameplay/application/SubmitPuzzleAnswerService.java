package com.escapehub.gameplay.application;

import com.escapehub.gameplay.domain.GameSession;
import com.escapehub.puzzle.application.PuzzleCatalog;
import com.escapehub.puzzle.domain.AnswerValidator;
import com.escapehub.puzzle.domain.PuzzleDefinition;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * US-022: a participant submits an answer to a puzzle of the current game session.
 *
 * <p>The server alone decides: it checks the participant and the session, validates the answer against the
 * puzzle's configured solution and only then records the puzzle as solved. The progress is kept in the shared game
 * state of the session (key {@code puzzle.<id>}), so there is a single source of truth, it is persisted like the
 * rest of the game and it reaches the other participants through the real-time updates. A wrong answer changes
 * nothing. A puzzle that is already solved is never solved again, so its effect happens exactly once. Everything is
 * scoped to the session in the request.</p>
 */
@Service
public class SubmitPuzzleAnswerService {

    /** Shared-state value stored for a solved puzzle. */
    public static final String SOLVED_VALUE = "solved";

    private final GameSessionRepository sessions;
    private final SharedStateService sharedState;
    private final GameStateService gameStates;
    private final PuzzleCatalog catalog;
    private final AnswerValidator validator = new AnswerValidator();

    /**
     * Creates the service.
     *
     * @param sessions session metadata
     * @param sharedState shared state of the sessions (also checks participation)
     * @param gameStates the service that persists and publishes game state changes
     * @param catalog configured puzzles
     */
    public SubmitPuzzleAnswerService(
            final GameSessionRepository sessions,
            final SharedStateService sharedState,
            final GameStateService gameStates,
            final PuzzleCatalog catalog) {
        this.sessions = sessions;
        this.sharedState = sharedState;
        this.gameStates = gameStates;
        this.catalog = catalog;
    }

    /**
     * Submits an answer for a puzzle.
     *
     * @param sessionId the participant's current game session
     * @param username the submitting participant
     * @param puzzleId the puzzle
     * @param answer the submitted answer
     * @return the feedback for the participant
     * @throws NoSuchElementException when the session or the puzzle does not exist
     * @throws org.springframework.security.access.AccessDeniedException when the user is not a participant
     * @throws IllegalStateException when the session is no longer active
     */
    public PuzzleSubmissionResult submit(
            final UUID sessionId, final String username, final String puzzleId, final String answer) {
        sharedState.requireParticipant(sessionId, username);
        final PuzzleDefinition puzzle = catalog.findById(puzzleId)
                .orElseThrow(() -> new NoSuchElementException("Ο γρίφος δεν βρέθηκε."));
        final String key = SharedStateService.PUZZLE_KEY_PREFIX + puzzle.id();
        // snapshot() also initializes the game of the session when needed
        if (sharedState.snapshot(sessionId, username).attributes().containsKey(key)) {
            return PuzzleSubmissionResult.alreadySolved(puzzle.id());
        }
        if (!validator.validate(answer, puzzle.solution())) {
            return PuzzleSubmissionResult.rejected(puzzle.id());
        }
        final GameSession session = sessions.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Η συνεδρία δεν βρέθηκε."));
        return gameStates.setAttributeIfAbsent(session.joinCode(), username.trim(), key, SOLVED_VALUE).isPresent()
                ? PuzzleSubmissionResult.accepted(puzzle.id())
                : PuzzleSubmissionResult.alreadySolved(puzzle.id());
    }
}
