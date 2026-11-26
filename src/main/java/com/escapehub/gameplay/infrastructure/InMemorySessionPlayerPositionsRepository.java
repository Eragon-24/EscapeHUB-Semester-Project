package com.escapehub.gameplay.infrastructure;

import com.escapehub.gameplay.application.SessionPlayerPositionsRepository;
import com.escapehub.gameplay.domain.movement.SessionPlayerPositions;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * Temporary in-memory adapter for tracked player positions.
 */
@Repository
@Profile("local")
public class InMemorySessionPlayerPositionsRepository implements SessionPlayerPositionsRepository {

    private final Map<UUID, SessionPlayerPositions> storage = new HashMap<>();

    @Override
    public synchronized SessionPlayerPositions save(
            final SessionPlayerPositions sessionPositions) {
        if (sessionPositions == null) {
            throw new IllegalArgumentException("Session positions must not be null");
        }
        storage.put(sessionPositions.getSessionId(), sessionPositions);
        return sessionPositions;
    }

    @Override
    public synchronized Optional<SessionPlayerPositions> findBySessionId(final UUID sessionId) {
        return Optional.ofNullable(storage.get(sessionId));
    }
}