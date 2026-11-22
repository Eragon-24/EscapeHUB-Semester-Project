package com.escapehub.gameplay.application;

/**
 * US-015: the server refused a requested shared-state change. The message is meant for the player.
 */
public class StateChangeRejectedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param message the reason, shown to the player
     */
    public StateChangeRejectedException(final String message) {
        super(message);
    }
}
