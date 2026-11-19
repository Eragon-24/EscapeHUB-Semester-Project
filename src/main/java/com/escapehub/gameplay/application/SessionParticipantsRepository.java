package com.escapehub.gameplay.application;

import com.escapehub.gameplay.domain.SessionParticipants;
import java.util.Optional;
import java.util.UUID;

/**
 * Storage port for game-session participants (US-012).
 */
public interface SessionParticipantsRepository {

    /**
     * Stores or updates participants for a session.
     *
     * @param participants participant state to store
     * @return stored participant state
     */
    SessionParticipants save(SessionParticipants participants);

    /**
     * Finds participant state by session identifier.
     *
     * @param sessionId unique session identifier
     * @return matching participant state, if present
     */
    Optional<SessionParticipants> findBySessionId(UUID sessionId);
}