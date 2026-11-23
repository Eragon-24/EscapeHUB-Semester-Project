package com.escapehub.gameplay.domain;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * US-012: Tracks active status and participants (max 6) for a GameSession.
 */
public class SessionParticipants {

    public static final int MAX_PLAYERS = 6;

    private final UUID sessionId;
    private final Set<String> players = new LinkedHashSet<>();
    private boolean active = true;

    /**
     * Creates participant tracking for a session with its creator as first player.
     *
     * @param sessionId unique session identifier
     * @param creatorUsername username of the session creator
     */
    public SessionParticipants(final UUID sessionId, final String creatorUsername) {
        this.sessionId = Objects.requireNonNull(sessionId, "Session id must not be null");
        if (creatorUsername == null || creatorUsername.isBlank()) {
            throw new IllegalArgumentException("Creator username must not be blank");
        }
        this.players.add(creatorUsername.trim());
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public boolean isActive() {
        return active;
    }

    /** Marks the session as inactive so no new players can join. */
    public void deactivate() {
        this.active = false;
    }

    public Set<String> getPlayers() {
        return Collections.unmodifiableSet(players);
    }

    /**
     * Adds a player to the session if active and below capacity.
     *
     * @param username authenticated player's username
     */
    public void addPlayer(final String username) {
        if (!active) {
            throw new IllegalStateException("Η συνεδρία δεν είναι πλέον ενεργή.");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username must not be blank");
        }
        final String cleanUsername = username.trim();
        if (players.contains(cleanUsername)) {
            return;
        }
        if (players.size() >= MAX_PLAYERS) {
            throw new IllegalStateException(
                    "Η συνεδρία είναι πλήρης (μέγιστο 6 παίκτες).");
        }
        players.add(cleanUsername);
    }

    /** Removes a player from the session. */
    public boolean removePlayer(final String username) {
        if (username == null || username.isBlank()) {
            return false;
        }
        return players.remove(username.trim());
    }
}
