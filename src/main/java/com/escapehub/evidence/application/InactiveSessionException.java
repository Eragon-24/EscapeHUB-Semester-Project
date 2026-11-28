package com.escapehub.evidence.application;

/**
 * Thrown when an operation is attempted on a game session that is no longer active.
 */
public class InactiveSessionException extends IllegalStateException {

    /**
     * Creates a new exception with the specified detail message.
     *
     * @param message detail message
     */
    public InactiveSessionException(final String message) {
        super(message);
    }
}