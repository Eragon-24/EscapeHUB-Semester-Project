package com.escapehub.gameplay.domain.movement;

/**
 * Axis-aligned rectangular obstacle in the room.
 *
 * @param minX left coordinate
 * @param minY top coordinate
 * @param maxX right coordinate
 * @param maxY bottom coordinate
 */
public record MovementObstacle(double minX, double minY, double maxX, double maxY) {

    /**
     * Ensures that the obstacle has finite coordinates and positive dimensions.
     */
    public MovementObstacle {
        if (!Double.isFinite(minX) || !Double.isFinite(minY)
                || !Double.isFinite(maxX) || !Double.isFinite(maxY)
                || minX >= maxX || minY >= maxY) {
            throw new IllegalArgumentException("Obstacle coordinates must define a valid rectangle");
        }
    }

    boolean intersects(final PlayerPosition position, final double characterRadius) {
        return position.x() + characterRadius > minX
                && position.x() - characterRadius < maxX
                && position.y() + characterRadius > minY
                && position.y() - characterRadius < maxY;
    }
}
