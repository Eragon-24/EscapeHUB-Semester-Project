package com.escapehub.puzzle.domain;

/**
 * US-004: αποτέλεσμα ελέγχου απάντησης με σαφές μήνυμα ανατροφοδότησης.
 *
 * @param correct true αν η απάντηση ήταν σωστή
 * @param message μήνυμα προς τον παίκτη (επιτυχία ή αποτυχία)
 */
public record AnswerResult(boolean correct, String message) {

    /** Μήνυμα επιτυχίας για σωστή απάντηση. */
    public static final String SUCCESS_MESSAGE = "Σωστή απάντηση! Μπορείς να προχωρήσεις.";

    /** Μήνυμα αποτυχίας για λάθος απάντηση. */
    public static final String FAILURE_MESSAGE = "Λάθος απάντηση. Δοκίμασε ξανά.";

    /** Αποτέλεσμα για σωστή απάντηση. */
    public static AnswerResult success() {
        return new AnswerResult(true, SUCCESS_MESSAGE);
    }

    /** Αποτέλεσμα για λάθος απάντηση. */
    public static AnswerResult failure() {
        return new AnswerResult(false, FAILURE_MESSAGE);
    }
}
