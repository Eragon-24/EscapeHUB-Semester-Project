package com.escapehub.gameplay.infrastructure;

import com.escapehub.gameplay.application.SessionParticipantsRepository;
import com.escapehub.gameplay.domain.SessionParticipants;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * Temporary in-memory adapter for session participants.
 */
@Repository
public class InMemorySessionParticipantsRepository implements SessionParticipantsRepository {

    private final Map<UUID, SessionParticipants> storage = new HashMap<>();

    @Override
    public synchronized SessionParticipants save(final SessionParticipants participants) {
        if (participants == null) {
            throw new IllegalArgumentException("Participants must not be null");
        }
        storage.put(participants.getSessionId(), participants);
        return participants;
    }

    @Override
    public synchronized Optional<SessionParticipants> findBySessionId(final UUID sessionId) {
        return Optional.ofNullable(storage.get(sessionId));
    }
}