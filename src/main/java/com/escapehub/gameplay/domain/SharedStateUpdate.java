package com.escapehub.gameplay.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * US-015: one accepted change of the shared game state, as delivered to the participants of a session.
 *
 * @param sessionId the game session the change belongs to
 * @param version position of this change in the session's history (1, 2, 3, ...)
 * @param key the state entry that changed
 * @param value the new value of that entry
 * @param author username of the participant whose request caused the change
 * @param at time at which the server accepted the change
 */
public record SharedStateUpdate(
        UUID sessionId,
        long version,
        String key,
        String value,
        String author,
        Instant at) {
}
