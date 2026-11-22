package com.escapehub.gameplay.application;

import java.util.UUID;

/**
 * US-015: extension point for the gameplay stories. Every {@code StateChangeRule} bean is consulted before a
 * change is applied. A rule rejects a change by throwing {@link StateChangeRejectedException}; a rejected change
 * is neither applied nor broadcast.
 */
@FunctionalInterface
public interface StateChangeRule {

    /**
     * Checks a requested change.
     *
     * @param sessionId the session
     * @param username the participant who requests the change
     * @param key the state entry
     * @param value the requested value
     * @throws StateChangeRejectedException when the change is not allowed
     */
    void check(UUID sessionId, String username, String key, String value);
}
