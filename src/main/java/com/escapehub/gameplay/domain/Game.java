package com.escapehub.gameplay.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class Game {

    public static final String INITIAL_ROOM_ID = "abandoned-laboratory";

    private final UUID sessionId;
    private String currentRoomId;
    private GameStatus status;
    private final Map<String, String> attributes = new HashMap<>();
    private long version;

    public Game(final UUID sessionId) {
        this.sessionId = Objects.requireNonNull(sessionId, "Session id must not be null");
        this.currentRoomId = INITIAL_ROOM_ID;
        this.status = GameStatus.IN_PROGRESS;
    }

    private Game(
            final UUID sessionId,
            final String currentRoomId,
            final GameStatus status,
            final Map<String, String> attributes,
            final long version) {
        this.sessionId = Objects.requireNonNull(sessionId, "Session id must not be null");
        if (currentRoomId == null || currentRoomId.isBlank()) {
            throw new IllegalArgumentException("Room id must not be blank");
        }
        if (version < 0) {
            throw new IllegalArgumentException("Version must not be negative");
        }
        this.currentRoomId = currentRoomId;
        this.status = Objects.requireNonNull(status, "Status must not be null");
        this.attributes.putAll(Objects.requireNonNull(attributes, "Attributes must not be null"));
        this.version = version;
    }

    public static Game restore(
            final UUID sessionId,
            final String currentRoomId,
            final GameStatus status,
            final Map<String, String> attributes,
            final long version) {
        return new Game(sessionId, currentRoomId, status, attributes, version);
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public synchronized void changeRoom(final String roomId) {
        if (roomId == null || roomId.isBlank()) {
            throw new IllegalArgumentException("Room id must not be blank");
        }
        currentRoomId = roomId.trim();
        version++;
    }

    public synchronized void putAttribute(final String key, final String value) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Attribute key must not be blank");
        }
        if (value == null) {
            throw new IllegalArgumentException("Attribute value must not be null");
        }
        attributes.put(key.trim(), value);
        version++;
    }

    public synchronized GameState snapshot() {
        return new GameState(sessionId, currentRoomId, status, attributes, version);
    }
}