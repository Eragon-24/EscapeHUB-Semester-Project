package com.escapehub.gameplay.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class Game {

    public static final String INITIAL_ROOM_ID = "abandoned-laboratory";

    private final UUID sessionId;
    private String currentRoomId = INITIAL_ROOM_ID;
    private GameStatus status = GameStatus.IN_PROGRESS;
    private final Map<String, String> attributes = new HashMap<>();
    private long version;


    public Game(final UUID sessionId) {
        this.sessionId = Objects.requireNonNull(sessionId, "Session id must not be null");
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