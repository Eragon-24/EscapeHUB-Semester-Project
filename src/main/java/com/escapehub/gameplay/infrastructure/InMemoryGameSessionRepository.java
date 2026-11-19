package com.escapehub.gameplay.infrastructure;

import com.escapehub.gameplay.application.GameSessionRepository;
import com.escapehub.gameplay.domain.GameSession;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * Temporary in-memory adapter for game-session metadata.
 *
 * <p>It can be replaced by a database-backed adapter without changing the
 * creation use case or the join-code lookup contract.</p>
 */
@Repository
public class InMemoryGameSessionRepository implements GameSessionRepository {

    private final Map<UUID, GameSession> sessions = new HashMap<>();

    @Override
    public synchronized GameSession save(final GameSession session) {
        if (session == null) {
            throw new IllegalArgumentException("Session must not be null");
        }

        for (GameSession storedSession : sessions.values()) {
            if (!storedSession.id().equals(session.id())
                    && storedSession.joinCode().equals(session.joinCode())) {
                throw new IllegalArgumentException("Join code is already in use");
            }
        }

        sessions.put(session.id(), session);
        return session;
    }

    @Override
    public synchronized boolean existsByJoinCode(final String joinCode) {
        return sessions.values().stream()
                .anyMatch(session -> session.joinCode().equals(joinCode));
    }

    @Override
    public synchronized Optional<GameSession> findByJoinCode(final String joinCode) {
        return sessions.values().stream()
                .filter(session -> session.joinCode().equals(joinCode))
                .findFirst();
    }

    @Override
    public synchronized Optional<GameSession> findById(final UUID id) {
        return Optional.ofNullable(sessions.get(id));
    }
}
