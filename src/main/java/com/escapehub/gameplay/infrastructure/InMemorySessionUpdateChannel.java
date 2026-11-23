package com.escapehub.gameplay.infrastructure;

import com.escapehub.gameplay.application.SessionUpdateChannel;
import com.escapehub.gameplay.application.UpdateSink;
import com.escapehub.gameplay.domain.SharedStateUpdate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

/**
 * US-015: single-server delivery channel. Subscribers are kept per session, so an update can only reach the
 * participants connected to the same session. A participant that cannot be reached is removed and never makes
 * the others fail.
 */
@Component
public class InMemorySessionUpdateChannel implements SessionUpdateChannel {

    private final Map<UUID, List<UpdateSink>> subscribers = new ConcurrentHashMap<>();

    @Override
    public Runnable subscribe(final UUID sessionId, final UpdateSink sink) {
        subscribers.compute(sessionId, (id, sinks) -> {
            final List<UpdateSink> current = sinks == null ? new CopyOnWriteArrayList<>() : sinks;
            current.add(sink);
            return current;
        });
        return () -> unsubscribe(sessionId, sink);
    }

    @Override
    public void publish(final SharedStateUpdate update) {
        final List<UpdateSink> sinks = subscribers.get(update.sessionId());
        if (sinks == null) {
            return;
        }
        for (final UpdateSink sink : sinks) {
            try {
                sink.send(update);
            } catch (Exception e) {
                // lost connection or completed stream: drop this participant only
                unsubscribe(update.sessionId(), sink);
            }
        }
    }

    /**
     * Returns how many participants are currently subscribed to a session (used by tests and diagnostics).
     *
     * @param sessionId the session
     * @return the number of subscribers
     */
    public int subscriberCount(final UUID sessionId) {
        final List<UpdateSink> sinks = subscribers.get(sessionId);
        return sinks == null ? 0 : sinks.size();
    }

    private void unsubscribe(final UUID sessionId, final UpdateSink sink) {
        subscribers.computeIfPresent(sessionId, (id, sinks) -> {
            sinks.remove(sink);
            return sinks.isEmpty() ? null : sinks;
        });
    }
}
