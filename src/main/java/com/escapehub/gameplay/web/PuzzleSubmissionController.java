package com.escapehub.gameplay.web;

import com.escapehub.gameplay.application.PuzzleSubmissionResult;
import com.escapehub.gameplay.application.SharedStateService;
import com.escapehub.gameplay.application.SubmitPuzzleAnswerService;
import com.escapehub.puzzle.application.PuzzleCatalog;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * US-022: lets a participant see the puzzles of their session and submit answers to them.
 *
 * <p>The answer is always checked on the server; the response only tells the participant whether it was accepted.
 * The puzzle's solution is never sent to the client.</p>
 */
@RestController
public class PuzzleSubmissionController {

    private final SubmitPuzzleAnswerService submitService;
    private final SharedStateService sharedState;
    private final PuzzleCatalog catalog;

    /**
     * Creates the controller.
     *
     * @param submitService the answer submission use case
     * @param sharedState used to check that the user takes part in the session
     * @param catalog the configured puzzles
     */
    public PuzzleSubmissionController(
            final SubmitPuzzleAnswerService submitService,
            final SharedStateService sharedState,
            final PuzzleCatalog catalog) {
        this.submitService = submitService;
        this.sharedState = sharedState;
        this.catalog = catalog;
    }

    /**
     * Lists the puzzles the participant can attempt (id and prompt only, never the solution).
     *
     * @param sessionId the participant's game session
     * @param principal the authenticated user
     * @return the available puzzles
     */
    @GetMapping("/game-sessions/{sessionId}/puzzles")
    public List<PuzzleView> puzzles(@PathVariable final UUID sessionId, final Principal principal) {
        sharedState.requireParticipant(sessionId, principal.getName());
        return catalog.findAll().stream().map(p -> new PuzzleView(p.id(), p.prompt())).toList();
    }

    /**
     * Submits an answer for a puzzle of the session.
     *
     * @param sessionId the participant's game session
     * @param puzzleId the puzzle
     * @param answer the submitted answer
     * @param principal the authenticated user
     * @return whether the answer was accepted, rejected, or the puzzle was already solved, with feedback
     */
    @PostMapping("/game-sessions/{sessionId}/puzzles/{puzzleId}/answer")
    public PuzzleSubmissionResult submit(
            @PathVariable final UUID sessionId,
            @PathVariable final String puzzleId,
            @RequestParam final String answer,
            final Principal principal) {
        return submitService.submit(sessionId, principal.getName(), puzzleId, answer);
    }

    /**
     * Maps an unknown session or puzzle to 404.
     *
     * @param e the exception
     * @return the error body
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> notFound(final NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }

    /**
     * Maps an inactive session to 409.
     *
     * @param e the exception
     * @return the error body
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> conflict(final IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
    }

    /**
     * A puzzle as shown to the players.
     *
     * @param id the puzzle id
     * @param prompt the question
     */
    public record PuzzleView(String id, String prompt) {
    }
}
