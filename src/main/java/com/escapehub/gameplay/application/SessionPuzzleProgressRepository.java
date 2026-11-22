package com.escapehub.gameplay.application;

import com.escapehub.gameplay.domain.SessionPuzzleProgress;
import java.util.UUID;

/**
 * US-022: storage port for the puzzle progress of game sessions.
 */
public interface SessionPuzzleProgressRepository {

    /**
     * Returns the progress of a session, creating an empty one on first access. The same instance is returned on
     * every call for the same session.
     *
     * @param sessionId the session
     * @return the session's puzzle progress
     */
    SessionPuzzleProgress forSession(UUID sessionId);
}
