package com.escapehub.gameplay.application;

import com.escapehub.gameplay.domain.GameSession;
import com.escapehub.gameplay.domain.ItemType;
import com.escapehub.puzzle.application.PuzzleCatalog;
import com.escapehub.puzzle.domain.AnswerValidator;
import com.escapehub.puzzle.domain.PuzzleDefinition;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * US-022: a participant submits an answer to a puzzle of the current game session.
 *
 * <p>The server alone decides: it checks the participant and the session and validates the answer against the
 * puzzle's configured solution. Only a correct answer completes the puzzle through
 * {@link GameStateService#completePuzzle}, which grants the reward items, unlocks the exit (US-024), persists the
 * progress and publishes it to the session. The shared game state is the single source of truth. A wrong answer
 * changes nothing, and a puzzle that is already completed is never completed again, so its effects happen exactly
 * once. Everything is scoped to the session in the request.</p>
 */
@Service
public class SubmitPuzzleAnswerService {

    /** Items the team receives for solving a puzzle (US-024). Puzzles without an entry give no item. */
    private static final Map<String, Set<ItemType>> REWARDS = Map.of("chest", Set.of(ItemType.LAB_KEY));

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
     * @param gameStates the service that changes, persists and publishes the game state
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
        // snapshot() also initializes the game of the session when needed
        if (sharedState.snapshot(sessionId, username).completedPuzzleIds().contains(puzzle.id())) {
            return PuzzleSubmissionResult.alreadySolved(puzzle.id());
        }
        if (!validator.validate(answer, puzzle.solution())) {
            return PuzzleSubmissionResult.rejected(puzzle.id());
        }
        final GameSession session = sessions.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Η συνεδρία δεν βρέθηκε."));
        final boolean completedNow = gameStates.completePuzzle(
                session.joinCode(), username.trim(), puzzle.id(), REWARDS.getOrDefault(puzzle.id(), Set.of()))
                .isPresent();
        return completedNow
                ? PuzzleSubmissionResult.accepted(puzzle.id())
                : PuzzleSubmissionResult.alreadySolved(puzzle.id());
    }
}
