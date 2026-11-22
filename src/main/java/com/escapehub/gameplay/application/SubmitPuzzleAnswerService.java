package com.escapehub.gameplay.application;

import com.escapehub.gameplay.domain.SessionPuzzleProgress;
import com.escapehub.puzzle.application.PuzzleCatalog;
import com.escapehub.puzzle.domain.AnswerValidator;
import com.escapehub.puzzle.domain.PuzzleDefinition;
import java.time.Clock;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * US-022: a participant submits an answer to a puzzle of the current game session.
 *
 * <p>The server alone decides: it checks the participant and the session, validates the answer against the
 * puzzle's configured solution and only then records the puzzle as solved. A wrong answer changes nothing. A
 * puzzle that is already solved is never solved again, so its effect (publishing the result to the session's
 * shared state, see US-015) happens exactly once. Everything is scoped to the session in the request.</p>
 */
@Service
public class SubmitPuzzleAnswerService {

    /** Prefix of the shared-state keys that carry puzzle progress (server-owned, see {@code SharedStateService}). */
    public static final String STATE_KEY_PREFIX = "puzzle.";

    /** Shared-state value published for a solved puzzle. */
    public static final String SOLVED_VALUE = "solved";

    private final SharedStateService sharedState;
    private final SessionPuzzleProgressRepository progressRepository;
    private final PuzzleCatalog catalog;
    private final AnswerValidator validator = new AnswerValidator();
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param sharedState shared state of the sessions (also checks participation)
     * @param progressRepository storage of the puzzle progress
     * @param catalog configured puzzles
     * @param clock the clock
     */
    public SubmitPuzzleAnswerService(
            final SharedStateService sharedState,
            final SessionPuzzleProgressRepository progressRepository,
            final PuzzleCatalog catalog,
            final Clock clock) {
        this.sharedState = sharedState;
        this.progressRepository = progressRepository;
        this.catalog = catalog;
        this.clock = clock;
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
        final SessionPuzzleProgress progress = progressRepository.forSession(sessionId);
        synchronized (progress) {
            if (progress.isSolved(puzzle.id())) {
                return PuzzleSubmissionResult.alreadySolved(puzzle.id());
            }
            if (!validator.validate(answer, puzzle.solution())) {
                return PuzzleSubmissionResult.rejected(puzzle.id());
            }
            final String solver = username.trim();
            progress.markSolved(puzzle.id(), solver, Instant.now(clock));
            sharedState.applyServerChange(sessionId, solver, STATE_KEY_PREFIX + puzzle.id(), SOLVED_VALUE);
            return PuzzleSubmissionResult.accepted(puzzle.id());
        }
    }
}
