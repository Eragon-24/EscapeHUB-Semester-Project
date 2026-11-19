package com.escapehub.gameplay.application;

import com.escapehub.gameplay.domain.GameSession;
import java.util.Optional;
import java.util.UUID;

/**
 * Storage port for game-session metadata.
 *
 * <p>The join-code lookup is exposed for US-012. This interface does not
 * implement joining or participant management.</p>
 */
public interface GameSessionRepository {

    /**
     * Stores a newly created session.
     *
     * @param session session metadata
     * @return the stored session
     */
    GameSession save(GameSession session);

    /**
     * Checks whether a join code is already assigned.
     *
     * @param joinCode code to check
     * @return true when the code is already in use
     */
    boolean existsByJoinCode(String joinCode);

    /**
     * Finds a session by its invite code for the future join-session use case.
     *
     * @param joinCode invite code
     * @return matching session, if present
     */
    Optional<GameSession> findByJoinCode(String joinCode);

    /**
     * Finds a session by its unique identifier.
     *
     * @param id session identifier
     * @return matching session, if present
     */
    Optional<GameSession> findById(UUID id);
}
