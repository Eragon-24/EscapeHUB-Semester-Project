package com.escapehub.gameplay.domain.movement;

import java.util.List;
import java.util.Objects;

/**
 * Defines the allowed coordinates for a player's centre and the room obstacles.
 *
 * <p>The bounds describe valid player-centre coordinates. Obstacle collisions account
 * for the player's radius.</p>
 */
public final class MovementArea {

    private final double minX;
    private final double minY;
    private final double maxX;
    private final double maxY;
    private final List<MovementObstacle> obstacles;

    /**
     * Creates movement constraints for a room.
     *
     * @param minX minimum allowed player-centre x coordinate
     * @param minY minimum allowed player-centre y coordinate
     * @param maxX maximum allowed player-centre x coordinate
     * @param maxY maximum allowed player-centre y coordinate
     * @param obstacles rectangular obstacles within the room
     */
    public MovementArea(
            final double minX,
            final double minY,
            final double maxX,
            final double maxY,
            final List<MovementObstacle> obstacles) {
        if (!Double.isFinite(minX) || !Double.isFinite(minY)
                || !Double.isFinite(maxX) || !Double.isFinite(maxY)
                || minX >= maxX || minY >= maxY) {
            throw new IllegalArgumentException("Movement bounds must define a valid area");
        }
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        this.obstacles = List.copyOf(Objects.requireNonNull(
                obstacles,
                "Movement obstacles must not be null"
        ));
    }

    boolean allows(final PlayerPosition position, final double characterRadius) {
        if (position.x() < minX || position.x() > maxX
                || position.y() < minY || position.y() > maxY) {
            return false;
        }

        return obstacles.stream().noneMatch(
                obstacle -> obstacle.intersects(position, characterRadius)
        );
    }
}
