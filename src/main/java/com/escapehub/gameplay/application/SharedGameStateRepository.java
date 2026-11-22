package com.escapehub.gameplay.application;

import com.escapehub.gameplay.domain.SharedGameState;
import java.util.UUID;

/**
 * US-015: storage port for the official shared state of game sessions.
 */
public interface SharedGameStateRepository {

    /**
     * Returns the state of a session, creating an empty one on first access. The same instance is returned on
     * every call for the same session.
     *
     * @param sessionId the session
     * @return the session's state
     */
    SharedGameState forSession(UUID sessionId);
}
