package com.escapehub.gameplay.domain;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * US-015: the official shared state of one game session. Only the server changes it; every accepted change
 * increases the version by one.
 *
 * <p>The class is not thread-safe: callers must synchronize on the instance (the application service does).</p>
 */
public final class SharedGameState {

    private final UUID sessionId;
    private final Map<String, String> values = new LinkedHashMap<>();
    private long version;

    /**
     * Creates an empty state (version 0) for a session.
     *
     * @param sessionId the game session
     */
    public SharedGameState(final UUID sessionId) {
        this.sessionId = Objects.requireNonNull(sessionId, "Session id must not be null");
    }

    /**
     * Applies an accepted change and returns the resulting update.
     *
     * @param key the state entry
     * @param value the new value
     * @param author username of the participant who requested the change
     * @param at time of acceptance
     * @return the update to publish
     */
    public SharedStateUpdate apply(final String key, final String value, final String author, final Instant at) {
        version++;
        values.put(key, value);
        return new SharedStateUpdate(sessionId, version, key, value, author, at);
    }

    /**
     * Returns an immutable copy of the current state.
     *
     * @return the snapshot
     */
    public SharedStateSnapshot snapshot() {
        return new SharedStateSnapshot(sessionId, version, Collections.unmodifiableMap(new LinkedHashMap<>(values)));
    }

    /**
     * Returns the id of the session this state belongs to.
     *
     * @return the session id
     */
    public UUID getSessionId() {
        return sessionId;
    }

    /**
     * Returns the current version.
     *
     * @return the version (0 when nothing changed yet)
     */
    public long getVersion() {
        return version;
    }
}
