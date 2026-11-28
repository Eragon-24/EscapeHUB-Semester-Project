package com.escapehub.evidence.application;

/**
 * Thrown when an evidence item has already been recorded in the session's shared journal.
 */
public class DuplicateEvidenceException extends IllegalStateException {

    /**
     * Creates a new exception with the specified detail message.
     *
     * @param message detail message
     */
    public DuplicateEvidenceException(final String message) {
        super(message);
    }

    /**
     * Creates a new exception with the specified detail message and cause.
     *
     * @param message detail message
     * @param cause   underlying cause
     */
    public DuplicateEvidenceException(final String message, final Throwable cause) {
        super(message, cause);
    }
}