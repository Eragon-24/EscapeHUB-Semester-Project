package com.escapehub.gameplay.application;

/**
 * US-022: the outcome of a puzzle answer submission, with clear feedback for the player.
 *
 * @param puzzleId the puzzle the answer was submitted for
 * @param status what happened to the submission
 * @param message feedback shown to the player
 */
public record PuzzleSubmissionResult(String puzzleId, Status status, String message) {

    /** What happened to a submitted answer. */
    public enum Status {
        /** The answer was correct and the puzzle is now solved for the whole session. */
        ACCEPTED,
        /** The answer was wrong; nothing changed. */
        REJECTED,
        /** The puzzle was already solved earlier; nothing changed and no effect is repeated. */
        ALREADY_SOLVED
    }

    /**
     * Creates the result of a correct answer.
     *
     * @param puzzleId the puzzle
     * @return the result
     */
    public static PuzzleSubmissionResult accepted(final String puzzleId) {
        return new PuzzleSubmissionResult(puzzleId, Status.ACCEPTED, "Σωστή απάντηση! Ο γρίφος λύθηκε.");
    }

    /**
     * Creates the result of a wrong answer.
     *
     * @param puzzleId the puzzle
     * @return the result
     */
    public static PuzzleSubmissionResult rejected(final String puzzleId) {
        return new PuzzleSubmissionResult(puzzleId, Status.REJECTED, "Λάθος απάντηση. Δοκίμασε ξανά.");
    }

    /**
     * Creates the result for a puzzle that was already solved.
     *
     * @param puzzleId the puzzle
     * @return the result
     */
    public static PuzzleSubmissionResult alreadySolved(final String puzzleId) {
        return new PuzzleSubmissionResult(
                puzzleId, Status.ALREADY_SOLVED, "Ο γρίφος έχει ήδη λυθεί από την ομάδα.");
    }
}
