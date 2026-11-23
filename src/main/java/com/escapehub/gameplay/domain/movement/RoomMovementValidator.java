package com.escapehub.gameplay.domain.movement;

import java.util.List;
import java.util.Objects;

/**
 * US-017: Server-side validator that enforces US-016 room movement bounds and obstacles.
 */
public final class RoomMovementValidator {

    private static final double ROOM_LEFT = 75;
    private static final double ROOM_TOP = 95;
    private static final double ROOM_RIGHT = 885;
    private static final double ROOM_BOTTOM = 570;
    private static final double PLAYER_RADIUS = 18;

    private static final MovementObstacle DESK = new MovementObstacle(150, 180, 420, 290);
    private static final MovementObstacle CHEST = new MovementObstacle(680, 195, 820, 305);
    private static final MovementObstacle EXIT_DOOR = new MovementObstacle(430, 92, 530, 142);

    private final MovementArea movementArea;
    private final double characterRadius;

    /**
     * Creates a validator for a specific room movement area and character radius.
     *
     * @param movementArea allowed room bounds and obstacles
     * @param characterRadius player half-size used for collision checks
     */
    public RoomMovementValidator(
            final MovementArea movementArea,
            final double characterRadius) {
        this.movementArea = Objects.requireNonNull(
                movementArea, "Movement area must not be null");
        if (!Double.isFinite(characterRadius) || characterRadius < 0) {
            throw new IllegalArgumentException(
                    "Character radius must be finite and non-negative");
        }
        this.characterRadius = characterRadius;
    }

    /**
     * Builds the default validator matching the abandoned laboratory room layout.
     *
     * @return validator configured with the room bounds and obstacles
     */
    public static RoomMovementValidator defaultLaboratory() {
        final MovementArea area = new MovementArea(
                ROOM_LEFT + PLAYER_RADIUS,
                ROOM_TOP + PLAYER_RADIUS,
                ROOM_RIGHT - PLAYER_RADIUS,
                ROOM_BOTTOM - PLAYER_RADIUS,
                List.of(DESK, CHEST, EXIT_DOOR)
        );
        return new RoomMovementValidator(area, PLAYER_RADIUS);
    }

    /**
     * Validates that a player position is within room bounds and does not intersect obstacles.
     *
     * @param position candidate player coordinates
     */
    public void validate(final PlayerPosition position) {
        Objects.requireNonNull(position, "Player position must not be null");
        if (!movementArea.allows(position, characterRadius)) {
            throw new IllegalArgumentException(
                    "Η θέση βρίσκεται εκτός των επιτρεπτών ορίων ή πάνω σε εμπόδιο.");
        }
    }
}