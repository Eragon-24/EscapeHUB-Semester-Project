package com.escapehub.evidence.application;

/**
 * Thrown when submitted clue data is missing or not eligible for the shared journal.
 */
public class InvalidEvidenceException extends IllegalArgumentException {

    /**
     * Creates a new exception with the specified detail message.
     *
     * @param message detail message
     */
    public InvalidEvidenceException(final String message) {
        super(message);
    }
}