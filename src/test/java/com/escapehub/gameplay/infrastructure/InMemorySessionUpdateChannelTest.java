package com.escapehub.gameplay.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.escapehub.gameplay.domain.SharedStateUpdate;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InMemorySessionUpdateChannelTest {

    private final InMemorySessionUpdateChannel channel = new InMemorySessionUpdateChannel();
    private final UUID sessionA = UUID.randomUUID();
    private final UUID sessionB = UUID.randomUUID();

    private SharedStateUpdate update(final UUID session, final long version) {
        return new SharedStateUpdate(session, version, "door", "open", "alice", Instant.parse("2026-11-22T10:00:00Z"));
    }

    @Test
    void allSubscribersOfTheSessionReceiveTheUpdate() {
        final List<SharedStateUpdate> first = new ArrayList<>();
        final List<SharedStateUpdate> second = new ArrayList<>();
        channel.subscribe(sessionA, first::add);
        channel.subscribe(sessionA, second::add);

        channel.publish(update(sessionA, 1));

        assertEquals(1, first.size());
        assertEquals(1, second.size());
    }

    @Test
    void subscribersOfAnotherSessionNeverReceiveIt() {
        final List<SharedStateUpdate> otherSession = new ArrayList<>();
        channel.subscribe(sessionB, otherSession::add);

        channel.publish(update(sessionA, 1));

        assertTrue(otherSession.isEmpty());
    }

    @Test
    void cancelledSubscriptionReceivesNothingMore() {
        final List<SharedStateUpdate> received = new ArrayList<>();
        final Runnable cancel = channel.subscribe(sessionA, received::add);
        channel.publish(update(sessionA, 1));

        cancel.run();
        channel.publish(update(sessionA, 2));

        assertEquals(1, received.size());
        assertEquals(0, channel.subscriberCount(sessionA));
    }

    @Test
    void lostConnectionIsDroppedAndTheOthersStillReceive() {
        final List<SharedStateUpdate> healthy = new ArrayList<>();
        channel.subscribe(sessionA, u -> {
            throw new IOException("connection lost");
        });
        channel.subscribe(sessionA, healthy::add);

        channel.publish(update(sessionA, 1));
        channel.publish(update(sessionA, 2));

        assertEquals(2, healthy.size());
        assertEquals(1, channel.subscriberCount(sessionA));
    }

    @Test
    void publishingWithoutSubscribersIsHarmless() {
        channel.publish(update(sessionA, 1));

        assertEquals(0, channel.subscriberCount(sessionA));
    }
}
