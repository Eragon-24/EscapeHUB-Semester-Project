package com.escapehub.gameplay.domain.movement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * US-017: Tracks live player positions within a game session and builds
 * views of other players' movements relative to a viewer.
 */
public class SessionPlayerPositions {

    private final UUID sessionId;
    private final Map<String, PlayerPosition> positions = new LinkedHashMap<>();

    /**
     * Creates a position tracker for a specific game session.
     *
     * @param sessionId unique session identifier
     */
    public SessionPlayerPositions(final UUID sessionId) {
        this.sessionId = Objects.requireNonNull(sessionId, "Session id must not be null");
    }

    public UUID getSessionId() {
        return sessionId;
    }

    /**
     * Updates or registers a player's current position in the session.
     *
     * @param username authenticated player's username
     * @param position validated room coordinates
     */
    public void updatePosition(final String username, final PlayerPosition position) {
        final String cleanUsername = normalizeUsername(username);
        Objects.requireNonNull(position, "Player position must not be null");
        positions.put(cleanUsername, position);
    }

    /**
     * Removes a player from the tracked room positions (e.g. when leaving).
     *
     * @param username player's username to remove
     */
    public void removePlayer(final String username) {
        final String cleanUsername = normalizeUsername(username);
        positions.remove(cleanUsername);
    }

    /**
     * Retains only positions belonging to currently active session participants.
     *
     * @param activePlayers set of usernames still participating in the session
     */
    public void retainPlayers(final Set<String> activePlayers) {
        Objects.requireNonNull(activePlayers, "Active players must not be null");
        positions.keySet().retainAll(activePlayers);
    }

    /**
     * Returns the positions of all players in the session except the viewer.
     *
     * @param viewerUsername username of the player viewing the room
     * @return unmodifiable list of other players and their positions
     */
    public List<OtherPlayerView> getOtherPlayers(final String viewerUsername) {
        final String cleanViewer = normalizeUsername(viewerUsername);
        final List<OtherPlayerView> others = new ArrayList<>();
        for (Map.Entry<String, PlayerPosition> entry : positions.entrySet()) {
            if (!entry.getKey().equals(cleanViewer)) {
                others.add(new OtherPlayerView(entry.getKey(), entry.getValue()));
            }
        }
        return Collections.unmodifiableList(others);
    }

    private String normalizeUsername(final String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username must not be blank");
        }
        return username.trim();
    }
}