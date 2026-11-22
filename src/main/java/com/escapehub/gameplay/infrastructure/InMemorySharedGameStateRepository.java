package com.escapehub.gameplay.infrastructure;

import com.escapehub.gameplay.application.SharedGameStateRepository;
import com.escapehub.gameplay.domain.SharedGameState;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

/**
 * US-015: temporary in-memory storage of the shared state. Cleared when the server restarts (persistence: W8).
 */
@Repository
public class InMemorySharedGameStateRepository implements SharedGameStateRepository {

    private final Map<UUID, SharedGameState> states = new ConcurrentHashMap<>();

    @Override
    public SharedGameState forSession(final UUID sessionId) {
        return states.computeIfAbsent(sessionId, SharedGameState::new);
    }
}
