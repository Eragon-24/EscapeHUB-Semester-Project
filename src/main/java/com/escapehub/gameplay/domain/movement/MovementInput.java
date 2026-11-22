package com.escapehub.gameplay.domain.movement;

/**
 * Direction requested by the player, independent of any input device.
 *
 * @param horizontal horizontal direction: -1, 0 or 1
 * @param vertical vertical direction: -1, 0 or 1
 */
public record MovementInput(int horizontal, int vertical) {

    /**
     * Ensures that each direction component is within the supported range.
     */
    public MovementInput {
        if (horizontal < -1 || horizontal > 1 || vertical < -1 || vertical > 1) {
            throw new IllegalArgumentException("Movement direction components must be -1, 0 or 1");
        }
    }

    /**
     * @return true when no movement direction is requested
     */
    public boolean isIdle() {
        return horizontal == 0 && vertical == 0;
    }
}
