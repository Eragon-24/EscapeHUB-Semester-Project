package com.escapehub.gameplay.domain;

import java.util.Set;

/**
 * Result of checking a player's access to a room exit.
 *
 * @param allowed whether all exit requirements are available
 * @param missingItems requirements not available to the player
 */
public record ExitAccessResult(boolean allowed, Set<ItemType> missingItems) {

    /**
     * Ensures that the missing-items set cannot be changed by callers.
     */
    public ExitAccessResult {
        if (missingItems == null) {
            throw new IllegalArgumentException("Missing items must not be null");
        }
        missingItems = Set.copyOf(missingItems);
    }
}
