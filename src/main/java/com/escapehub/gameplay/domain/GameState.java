package com.escapehub.gameplay.domain;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record GameState(
        UUID sessionId,
        String currentRoomId,
        GameStatus status,
        Map<String, String> attributes,
        long version) {

    public GameState {
        Objects.requireNonNull(sessionId, "Session id must not be null");
        Objects.requireNonNull(currentRoomId, "Room id must not be null");
        Objects.requireNonNull(status, "Status must not be null");
        attributes = Map.copyOf(attributes);
    }
}