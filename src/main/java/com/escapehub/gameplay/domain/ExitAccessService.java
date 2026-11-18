package com.escapehub.gameplay.domain;

import java.util.Collection;
import java.util.Set;

/**
 * Stateless service that checks whether a player can use a room exit.
 */
public class ExitAccessService {

    /**
     * Checks the required items for an exit against a collection owned by the
     * current game session or, in the future, by a Player/Inventory.
     *
     * @param exit the exit to check
     * @param playerItems items available to the player
     * @return access result and all missing items
     */
    public ExitAccessResult checkAccess(
            final RoomExit exit,
            final Collection<ItemType> playerItems) {
        if (exit == null) {
            throw new IllegalArgumentException("Exit must not be null");
        }
        if (playerItems == null) {
            throw new IllegalArgumentException("Player items must not be null");
        }

        final Set<ItemType> availableItems = Set.copyOf(playerItems);
        final Set<ItemType> missingItems = exit.getRequiredItems().stream()
                .filter(item -> !availableItems.contains(item))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());

        return new ExitAccessResult(missingItems.isEmpty(), missingItems);
    }
}
