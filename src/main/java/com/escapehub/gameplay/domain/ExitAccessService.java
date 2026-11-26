package com.escapehub.gameplay.domain;

import java.util.Collection;
import java.util.Set;

/**
 * Stateless service that checks whether session-shared progress satisfies an exit's requirements.
 */
public class ExitAccessService {

    /**
     * Checks the required items for an exit against a session's collected items.
     *
     * @param exit the exit to check
     * @param playerItems items collected in the current session
     * @return access result and all missing items
     */
    public ExitAccessResult checkAccess(
            final RoomExit exit,
            final Collection<ItemType> playerItems) {
        return checkAccess(exit, Set.of(), playerItems);
    }

    /**
     * Checks session-shared puzzle completions and collected items for an exit.
     *
     * @param exit the exit to check
     * @param completedPuzzleIds puzzles completed in the current session
     * @param collectedItems items collected in the current session
     * @return access result and all missing requirements
     */
    public ExitAccessResult checkAccess(
            final RoomExit exit,
            final Collection<String> completedPuzzleIds,
            final Collection<ItemType> collectedItems) {
        if (exit == null) {
            throw new IllegalArgumentException("Exit must not be null");
        }
        if (completedPuzzleIds == null || collectedItems == null) {
            throw new IllegalArgumentException("Session progress must not be null");
        }

        final Set<ItemType> availableItems = Set.copyOf(collectedItems);
        final Set<ItemType> missingItems = exit.getRequiredItems().stream()
                .filter(item -> !availableItems.contains(item))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        final Set<String> completed = Set.copyOf(completedPuzzleIds);
        final Set<String> missingPuzzleIds = exit.getRequiredPuzzleIds().stream()
                .filter(puzzleId -> !completed.contains(puzzleId))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());

        return new ExitAccessResult(
                missingItems.isEmpty() && missingPuzzleIds.isEmpty(),
                missingItems,
                missingPuzzleIds);
    }
}
