package com.escapehub.gameplay.infrastructure;

import com.escapehub.gameplay.application.SessionPuzzleProgressRepository;
import com.escapehub.gameplay.domain.SessionPuzzleProgress;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

/**
 * US-022: temporary in-memory storage of the puzzle progress. Cleared when the server restarts (persistence: W8).
 */
@Repository
public class InMemorySessionPuzzleProgressRepository implements SessionPuzzleProgressRepository {

    private final Map<UUID, SessionPuzzleProgress> progress = new ConcurrentHashMap<>();

    @Override
    public SessionPuzzleProgress forSession(final UUID sessionId) {
        return progress.computeIfAbsent(sessionId, SessionPuzzleProgress::new);
    }
}
