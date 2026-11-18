package com.escapehub.gameplay.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Metadata for a created game session.
 *
 * <p>Participant membership and live gameplay state are deliberately kept out
 * of this model so their respective user stories can build on this foundation.</p>
 *
 * @param id unique session identifier
 * @param joinCode short code that can be used by the join-session use case
 * @param creatorUsername username of the authenticated session creator
 * @param createdAt time at which the session was created
 */
public record GameSession(
        UUID id,
        String joinCode,
        String creatorUsername,
        Instant createdAt) {

    private static final String JOIN_CODE_PATTERN = "[A-HJ-NP-Z2-9]{6}";

    public GameSession {
        Objects.requireNonNull(id, "Session id must not be null");
        Objects.requireNonNull(joinCode, "Join code must not be null");
        Objects.requireNonNull(creatorUsername, "Creator username must not be null");
        Objects.requireNonNull(createdAt, "Creation time must not be null");

        joinCode = joinCode.trim().toUpperCase(Locale.ROOT);
        creatorUsername = creatorUsername.trim();

        if (!joinCode.matches(JOIN_CODE_PATTERN)) {
            throw new IllegalArgumentException("Join code must contain exactly six valid characters");
        }
        if (creatorUsername.isBlank()) {
            throw new IllegalArgumentException("Creator username must not be blank");
        }
    }
}
