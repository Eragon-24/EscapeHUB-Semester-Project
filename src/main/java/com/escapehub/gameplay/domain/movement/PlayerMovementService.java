package com.escapehub.gameplay.domain.movement;

import java.util.Objects;

/**
 * Applies reusable, UI-independent movement rules to a player's position.
 *
 * <p>The requested distance is the movement amount for one update step.
 * Horizontal and vertical movement are evaluated separately so a player can
 * slide along an obstacle when only one axis is blocked.</p>
 */
public final class PlayerMovementService {

    private final MovementArea movementArea;
    private final double characterRadius;

    /**
     * Creates a movement service for a specific room area and character size.
     *
     * @param movementArea allowed room area and its obstacles
     * @param characterRadius half-size used for obstacle collision checks
     */
    public PlayerMovementService(
            final MovementArea movementArea,
            final double characterRadius) {
        this.movementArea = Objects.requireNonNull(
                movementArea,
                "Movement area must not be null"
        );
        if (!Double.isFinite(characterRadius) || characterRadius < 0) {
            throw new IllegalArgumentException("Character radius must be finite and non-negative");
        }
        this.characterRadius = characterRadius;
    }

    /**
     * Calculates the next player position for a requested direction.
     *
     * @param currentPosition current player position
     * @param input requested horizontal and vertical directions
     * @param distance movement amount for this update step
     * @return the new position after applying bounds and obstacle checks
     */
    public PlayerPosition move(
            final PlayerPosition currentPosition,
            final MovementInput input,
            final double distance) {
        Objects.requireNonNull(currentPosition, "Current player position must not be null");
        Objects.requireNonNull(input, "Movement input must not be null");
        if (!Double.isFinite(distance) || distance < 0) {
            throw new IllegalArgumentException("Movement distance must be finite and non-negative");
        }
        if (input.isIdle() || distance == 0) {
            return currentPosition;
        }

        final double directionLength = Math.hypot(input.horizontal(), input.vertical());
        final double deltaX = input.horizontal() / directionLength * distance;
        final double deltaY = input.vertical() / directionLength * distance;
        PlayerPosition updatedPosition = currentPosition;

        final PlayerPosition horizontalCandidate = new PlayerPosition(
                updatedPosition.x() + deltaX,
                updatedPosition.y()
        );
        if (movementArea.allows(horizontalCandidate, characterRadius)) {
            updatedPosition = horizontalCandidate;
        }

        final PlayerPosition verticalCandidate = new PlayerPosition(
                updatedPosition.x(),
                updatedPosition.y() + deltaY
        );
        if (movementArea.allows(verticalCandidate, characterRadius)) {
            updatedPosition = verticalCandidate;
        }

        return updatedPosition;
    }
}
