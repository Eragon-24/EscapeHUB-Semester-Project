package com.escapehub.gameplay.domain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class Game {

    public static final String INITIAL_ROOM_ID = "abandoned-laboratory";

    private final UUID sessionId;
    private String currentRoomId;
    private GameStatus status;
    private final Map<String, String> attributes = new HashMap<>();
    private final Set<String> completedPuzzleIds = new HashSet<>();
    private final Set<ItemType> collectedItems = new HashSet<>();
    private boolean exitUnlocked;
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

    public static Game restore(final GameState state) {
        Objects.requireNonNull(state, "Game state must not be null");
        final Game game = new Game(state.sessionId());
        game.currentRoomId = state.currentRoomId();
        game.status = state.status();
        game.attributes.putAll(state.attributes());
        game.completedPuzzleIds.addAll(state.completedPuzzleIds());
        game.collectedItems.addAll(state.collectedItems());
        game.exitUnlocked = state.exitUnlocked();
        game.version = state.version();
        return game;
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

    public synchronized boolean completePuzzle(final String puzzleId) {
        if (puzzleId == null || puzzleId.isBlank()) {
            throw new IllegalArgumentException("Puzzle id must not be blank");
        }
        final boolean changed = completedPuzzleIds.add(puzzleId.trim());
        if (changed) {
            version++;
        }
        return changed;
    }

    public synchronized boolean collectItem(final ItemType item) {
        if (item == null) {
            throw new IllegalArgumentException("Item must not be null");
        }
        final boolean changed = collectedItems.add(item);
        if (changed) {
            version++;
        }
        return changed;
    }

    public synchronized boolean unlockExit() {
        if (exitUnlocked) {
            return false;
        }
        exitUnlocked = true;
        version++;
        return true;
    }

    public synchronized GameState snapshot() {
        return new GameState(
                sessionId,
                currentRoomId,
                status,
                attributes,
                completedPuzzleIds,
                collectedItems,
                exitUnlocked,
                version);
    }
}