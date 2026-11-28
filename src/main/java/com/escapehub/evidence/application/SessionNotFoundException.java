package com.escapehub.evidence.application;

/**
 * Thrown when a requested game session cannot be found.
 */
public class SessionNotFoundException extends IllegalArgumentException {

    /**
     * Creates a new exception with the specified detail message.
     *
     * @param message detail message
     */
    public SessionNotFoundException(final String message) {
        super(message);
    }
}