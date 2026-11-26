package com.escapehub.gameplay.domain;

import java.util.Set;

/**
 * Result of checking a player's access to a room exit.
 *
 * @param allowed whether all exit requirements are available
 * @param missingItems requirements not available to the player
 * @param missingPuzzleIds required puzzles that have not been completed
 */
public record ExitAccessResult(
        boolean allowed,
        Set<ItemType> missingItems,
        Set<String> missingPuzzleIds) {

    /**
     * Ensures that the missing-items set cannot be changed by callers.
     */
    public ExitAccessResult {
        if (missingItems == null || missingPuzzleIds == null) {
            throw new IllegalArgumentException("Missing requirements must not be null");
        }
        missingItems = Set.copyOf(missingItems);
        missingPuzzleIds = Set.copyOf(missingPuzzleIds);
    }
}
