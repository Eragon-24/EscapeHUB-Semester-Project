package com.escapehub.gameplay.domain.movement;

import java.util.Objects;

/**
 * US-017: Immutable snapshot of another player's position in a game session.
 *
 * @param username other player's username
 * @param position current room coordinates of the other player
 */
public record OtherPlayerView(String username, PlayerPosition position) {

    /**
     * Validates that the username is not blank and the position is not null.
     */
    public OtherPlayerView {
        Objects.requireNonNull(username, "Username must not be null");
        Objects.requireNonNull(position, "Player position must not be null");
        username = username.trim();
        if (username.isBlank()) {
            throw new IllegalArgumentException("Username must not be blank");
        }
    }
}