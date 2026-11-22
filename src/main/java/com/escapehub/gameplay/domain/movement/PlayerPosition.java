package com.escapehub.gameplay.domain.movement;

/**
 * Immutable position of a player in the room coordinate system.
 *
 * @param x horizontal coordinate
 * @param y vertical coordinate
 */
public record PlayerPosition(double x, double y) {

    /**
     * Ensures that the position contains valid finite coordinates.
     */
    public PlayerPosition {
        if (!Double.isFinite(x) || !Double.isFinite(y)) {
            throw new IllegalArgumentException("Player coordinates must be finite");
        }
    }
}
