package com.escapehub.gameplay.application;

import com.escapehub.gameplay.domain.movement.SessionPlayerPositions;
import java.util.Optional;
import java.util.UUID;

/**
 * Storage port for tracked player positions per game session (US-017).
 */
public interface SessionPlayerPositionsRepository {

    /**
     * Stores or updates tracked positions for a session.
     *
     * @param sessionPositions session position state
     * @return stored session position state
     */
    SessionPlayerPositions save(SessionPlayerPositions sessionPositions);

    /**
     * Finds tracked player positions by session identifier.
     *
     * @param sessionId unique session identifier
     * @return matching position tracker, if present
     */
    Optional<SessionPlayerPositions> findBySessionId(UUID sessionId);
}