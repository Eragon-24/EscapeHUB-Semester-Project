
package com.escapehub.gameplay.domain.movement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class SessionPlayerPositions {

    private final UUID sessionId;
    private final Map<String, PlayerPosition> positions = new LinkedHashMap<>();
    private final Set<String> hiddenPlayers = new HashSet<>();

    public SessionPlayerPositions(final UUID sessionId) {
        this.sessionId = Objects.requireNonNull(sessionId, "Session id must not be null");
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public Map<String, PlayerPosition> getPositions() {
        return Collections.unmodifiableMap(positions);
    }

    public void updatePosition(final String username, final PlayerPosition position) {
        final String cleanUsername = normalizeUsername(username);
        Objects.requireNonNull(position, "Player position must not be null");

        positions.put(cleanUsername, position);
        hiddenPlayers.removeIf(name -> name.equalsIgnoreCase(cleanUsername));
    }

    /**
     * Hides a player without deleting their last known position.
     */
    public void removePlayer(final String username) {
        final String cleanUsername = normalizeUsername(username);

        if (positions.keySet().stream()
                .anyMatch(name -> name.equalsIgnoreCase(cleanUsername))) {
            hiddenPlayers.add(cleanUsername);
        }
    }

    public boolean isVisible(final String username) {
        final String cleanUsername = normalizeUsername(username);

        return hiddenPlayers.stream()
                .noneMatch(name -> name.equalsIgnoreCase(cleanUsername));
    }

    public void retainPlayers(final Set<String> activePlayers) {
        Objects.requireNonNull(activePlayers, "Active players must not be null");

        positions.keySet().removeIf(username ->
                activePlayers.stream().noneMatch(
                        name -> name.equalsIgnoreCase(username)));

        hiddenPlayers.removeIf(username ->
                positions.keySet().stream().noneMatch(
                        name -> name.equalsIgnoreCase(username)));
    }

    public List<OtherPlayerView> getOtherPlayers(final String viewerUsername) {
        final String cleanViewer = normalizeUsername(viewerUsername);
        final List<OtherPlayerView> others = new ArrayList<>();

        for (Map.Entry<String, PlayerPosition> entry : positions.entrySet()) {
            if (!entry.getKey().equalsIgnoreCase(cleanViewer)
                    && isVisible(entry.getKey())) {
                others.add(new OtherPlayerView(entry.getKey(), entry.getValue()));
            }
        }

        return Collections.unmodifiableList(others);
    }

    public List<OtherPlayerView> getOtherPlayers(
            final String viewerUsername, final Set<String> activePlayers) {
        final String cleanViewer = normalizeUsername(viewerUsername);
        Objects.requireNonNull(activePlayers, "Active players must not be null");

        final List<OtherPlayerView> others = new ArrayList<>();

        for (Map.Entry<String, PlayerPosition> entry : positions.entrySet()) {
            boolean active = activePlayers.stream().anyMatch(
                    name -> name.equalsIgnoreCase(entry.getKey()));

            if (active
                    && isVisible(entry.getKey())
                    && !entry.getKey().equalsIgnoreCase(cleanViewer)) {
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
