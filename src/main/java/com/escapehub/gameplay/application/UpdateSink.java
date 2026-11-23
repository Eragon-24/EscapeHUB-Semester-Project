package com.escapehub.gameplay.application;

import com.escapehub.gameplay.domain.SharedStateUpdate;
import java.io.IOException;

/**
 * US-015: a connected participant's receiving end. The transport (Server-Sent Events) is hidden behind this
 * interface, so the application layer does not depend on it.
 */
@FunctionalInterface
public interface UpdateSink {

    /**
     * Delivers an update to the participant.
     *
     * @param update the update
     * @throws IOException when the participant is no longer reachable
     */
    void send(SharedStateUpdate update) throws IOException;
}
