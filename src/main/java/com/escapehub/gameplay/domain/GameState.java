package com.escapehub.gameplay.domain;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record GameState(
        UUID sessionId,
        String currentRoomId,
        GameStatus status,
        Map<String, String> attributes,
        Set<String> completedPuzzleIds,
        Set<ItemType> collectedItems,
        boolean exitUnlocked,
        long version) {

    public GameState {
        Objects.requireNonNull(sessionId, "Session id must not be null");
        Objects.requireNonNull(currentRoomId, "Room id must not be null");
        Objects.requireNonNull(status, "Status must not be null");
        attributes = Map.copyOf(attributes);
        completedPuzzleIds = Set.copyOf(completedPuzzleIds);
        collectedItems = Set.copyOf(collectedItems);
    }
}