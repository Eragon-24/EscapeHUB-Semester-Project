package com.escapehub.gameplay.application;

import com.escapehub.gameplay.domain.SharedStateUpdate;
import java.util.UUID;

/**
 * US-015: delivery channel for shared-state updates, scoped per game session. A subscription only ever receives
 * the updates of the session it subscribed to.
 */
public interface SessionUpdateChannel {

    /**
     * Subscribes a participant to the updates of one session.
     *
     * @param sessionId the session
     * @param sink where the updates are delivered
     * @return a handle that cancels the subscription when run
     */
    Runnable subscribe(UUID sessionId, UpdateSink sink);

    /**
     * Delivers an update to every subscriber of its session, and to nobody else. A subscriber that cannot be
     * reached is dropped without affecting the others.
     *
     * @param update the update
     */
    void publish(SharedStateUpdate update);
}
