package com.escapehub.gameplay.application;

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
 * state of the session (the {@code Game} of US-013, key {@code puzzle.<id>}), so there is a single source of truth
 * and the result reaches the other participants through the real-time updates of US-015. A wrong answer changes
 * nothing. A puzzle that is already solved is never solved again, so its effect happens exactly once. Everything is
 * scoped to the session in the request.</p>
 */
@Service
public class SubmitPuzzleAnswerService {

    /** Prefix of the shared-state keys that carry puzzle progress (server-owned, see {@code SharedStateService}). */
    public static final String STATE_KEY_PREFIX = "puzzle.";

    /** Shared-state value stored for a solved puzzle. */
    public static final String SOLVED_VALUE = "solved";

    private final SharedStateService sharedState;
    private final PuzzleCatalog catalog;
    private final AnswerValidator validator = new AnswerValidator();

    /**
     * Creates the service.
     *
     * @param sharedState shared state of the sessions (also checks participation)
     * @param catalog configured puzzles
     */
    public SubmitPuzzleAnswerService(final SharedStateService sharedState, final PuzzleCatalog catalog) {
        this.sharedState = sharedState;
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
        sharedState.requireActiveParticipant(sessionId, username);
        final PuzzleDefinition puzzle = catalog.findById(puzzleId)
                .orElseThrow(() -> new NoSuchElementException("Ο γρίφος δεν βρέθηκε."));
        final String key = STATE_KEY_PREFIX + puzzle.id();
        if (sharedState.valueOf(sessionId, username, key).isPresent()) {
            return PuzzleSubmissionResult.alreadySolved(puzzle.id());
        }
        if (!validator.validate(answer, puzzle.solution())) {
            return PuzzleSubmissionResult.rejected(puzzle.id());
        }
        // Check and set happen as one step: of several simultaneous correct answers exactly one is accepted.
        return sharedState.applyServerChangeIfAbsent(sessionId, username, key, SOLVED_VALUE).isPresent()
                ? PuzzleSubmissionResult.accepted(puzzle.id())
                : PuzzleSubmissionResult.alreadySolved(puzzle.id());
    }
}
