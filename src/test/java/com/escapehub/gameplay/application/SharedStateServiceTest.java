package com.escapehub.gameplay.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.escapehub.gameplay.domain.GameSession;
import com.escapehub.gameplay.domain.SessionParticipants;
import com.escapehub.gameplay.domain.SharedStateSnapshot;
import com.escapehub.gameplay.domain.SharedStateUpdate;
import com.escapehub.gameplay.infrastructure.InMemoryGameSessionRepository;
import com.escapehub.gameplay.infrastructure.InMemorySessionParticipantsRepository;
import com.escapehub.gameplay.infrastructure.InMemorySessionUpdateChannel;
import com.escapehub.gameplay.infrastructure.InMemorySharedGameStateRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class SharedStateServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-22T10:00:00Z"), ZoneOffset.UTC);

    private InMemoryGameSessionRepository sessions;
    private InMemorySessionParticipantsRepository participants;
    private InMemorySessionUpdateChannel channel;
    private List<StateChangeRule> rules;
    private SharedStateService service;
    private JoinGameSessionService joinService;
    private GameSession session;

    @BeforeEach
    void setUp() {
        sessions = new InMemoryGameSessionRepository();
        participants = new InMemorySessionParticipantsRepository();
        channel = new InMemorySessionUpdateChannel();
        rules = new ArrayList<>();
        service = new SharedStateService(
                sessions, participants, new InMemorySharedGameStateRepository(), channel, rules, clock);
        joinService = new JoinGameSessionService(sessions, participants);
        session = new CreateGameSessionService(sessions).createSession("alice");
        joinService.joinSession(session.joinCode(), "bob");
    }

    private List<SharedStateUpdate> listenTo(final UUID sessionId) {
        final List<SharedStateUpdate> received = new ArrayList<>();
        channel.subscribe(sessionId, received::add);
        return received;
    }

    @Test
    void participantChangeIsAppliedAndPublishedToTheSession() {
        final List<SharedStateUpdate> received = listenTo(session.id());

        final SharedStateUpdate update = service.applyChange(session.id(), "alice", "door", "open");

        assertEquals(1, update.version());
        assertEquals("alice", update.author());
        assertEquals(List.of(update), received);
        assertEquals("open", service.snapshot(session.id(), "bob").values().get("door"));
    }

    @Test
    void otherParticipantsReceiveTheUpdateWithoutAskingForIt() {
        final List<SharedStateUpdate> bobsView = listenTo(session.id());

        service.applyChange(session.id(), "alice", "door", "open");
        service.applyChange(session.id(), "alice", "lamp", "on");

        assertEquals(2, bobsView.size());
        assertEquals("lamp", bobsView.get(1).key());
        assertEquals(2, bobsView.get(1).version());
    }

    @Test
    void creatorCanChangeStateEvenWithoutAJoinRecord() {
        final GameSession solo = new CreateGameSessionService(sessions).createSession("carol");

        assertEquals(1, service.applyChange(solo.id(), "carol", "door", "open").version());
    }

    @Test
    void nonParticipantCannotChangeOrReadAndNothingIsPublished() {
        final List<SharedStateUpdate> received = listenTo(session.id());

        assertThrows(AccessDeniedException.class, () -> service.applyChange(session.id(), "mallory", "door", "open"));
        assertThrows(AccessDeniedException.class, () -> service.snapshot(session.id(), "mallory"));
        assertThrows(AccessDeniedException.class, () -> service.requireParticipant(session.id(), " "));
        assertTrue(received.isEmpty());
        assertEquals(0, service.snapshot(session.id(), "alice").version());
    }

    @Test
    void unknownSessionIsReported() {
        assertThrows(NoSuchElementException.class,
                () -> service.applyChange(UUID.randomUUID(), "alice", "door", "open"));
    }

    @Test
    void updatesNeverLeakToAnotherSession() {
        final GameSession other = new CreateGameSessionService(sessions).createSession("dave");
        final List<SharedStateUpdate> otherSessionView = listenTo(other.id());

        service.applyChange(session.id(), "alice", "door", "open");

        assertTrue(otherSessionView.isEmpty());
        assertTrue(service.snapshot(other.id(), "dave").values().isEmpty());
    }

    @Test
    void participantOfOneSessionCannotChangeAnotherSession() {
        final GameSession other = new CreateGameSessionService(sessions).createSession("dave");

        assertThrows(AccessDeniedException.class, () -> service.applyChange(other.id(), "alice", "door", "open"));
        assertTrue(service.snapshot(other.id(), "dave").values().isEmpty());
    }

    @Test
    void changeRefusedByARuleIsNotAppliedNorBroadcast() {
        rules.add((sessionId, user, key, value) -> {
            if ("exit".equals(key)) {
                throw new StateChangeRejectedException("Η έξοδος είναι κλειδωμένη.");
            }
        });
        service = new SharedStateService(
                sessions, participants, new InMemorySharedGameStateRepository(), channel, rules, clock);
        final List<SharedStateUpdate> received = listenTo(session.id());

        final StateChangeRejectedException error = assertThrows(StateChangeRejectedException.class,
                () -> service.applyChange(session.id(), "alice", "exit", "open"));

        assertEquals("Η έξοδος είναι κλειδωμένη.", error.getMessage());
        assertTrue(received.isEmpty());
        assertEquals(0, service.snapshot(session.id(), "alice").version());
        assertEquals(1, service.applyChange(session.id(), "alice", "door", "open").version());
    }

    @Test
    void malformedRequestsAreRejected() {
        final String tooLong = "x".repeat(SharedStateService.MAX_VALUE_LENGTH + 1);

        assertThrows(StateChangeRejectedException.class, () -> service.applyChange(session.id(), "alice", null, "v"));
        assertThrows(StateChangeRejectedException.class, () -> service.applyChange(session.id(), "alice", "", "v"));
        assertThrows(StateChangeRejectedException.class,
                () -> service.applyChange(session.id(), "alice", "bad key!", "v"));
        assertThrows(StateChangeRejectedException.class, () -> service.applyChange(session.id(), "alice", "k", null));
        assertThrows(StateChangeRejectedException.class,
                () -> service.applyChange(session.id(), "alice", "k", tooLong));
        assertEquals(0, service.snapshot(session.id(), "alice").version());
    }

    @Test
    void inactiveSessionRejectsChanges() {
        final SessionParticipants current = participants.findBySessionId(session.id()).orElseThrow();
        current.deactivate();
        participants.save(current);

        assertThrows(IllegalStateException.class, () -> service.applyChange(session.id(), "alice", "door", "open"));
    }

    @Test
    void lostConnectionOfOneParticipantDoesNotBreakTheSession() {
        channel.subscribe(session.id(), u -> {
            throw new java.io.IOException("connection lost");
        });
        final List<SharedStateUpdate> healthy = listenTo(session.id());

        final SharedStateUpdate first = service.applyChange(session.id(), "alice", "door", "open");
        final SharedStateUpdate second = service.applyChange(session.id(), "bob", "lamp", "on");

        assertEquals(2, second.version());
        assertEquals(List.of(first, second), healthy);
    }

    @Test
    void reconnectingParticipantGetsTheCurrentStateInASnapshot() {
        service.applyChange(session.id(), "alice", "door", "open");
        service.applyChange(session.id(), "alice", "lamp", "on");

        final SharedStateSnapshot snapshot = service.snapshot(session.id(), "bob");

        assertEquals(2, snapshot.version());
        assertEquals("open", snapshot.values().get("door"));
        assertEquals("on", snapshot.values().get("lamp"));
    }

    @Test
    void clientCannotWriteServerOwnedKeys() {
        final List<SharedStateUpdate> received = listenTo(session.id());

        assertThrows(StateChangeRejectedException.class,
                () -> service.applyChange(session.id(), "alice", "puzzle.chest", "solved"));

        assertTrue(received.isEmpty());
        assertEquals(0, service.snapshot(session.id(), "alice").version());
    }

    @Test
    void serverCanWriteServerOwnedKeysAndTheyAreBroadcast() {
        final List<SharedStateUpdate> received = listenTo(session.id());

        final SharedStateUpdate update = service.applyServerChange(session.id(), "alice", "puzzle.chest", "solved");

        assertEquals(List.of(update), received);
        assertEquals("solved", service.snapshot(session.id(), "bob").values().get("puzzle.chest"));
    }

    @Test
    void serverChangeStillRequiresAnActiveParticipantAndAValidKey() {
        assertThrows(AccessDeniedException.class,
                () -> service.applyServerChange(session.id(), "mallory", "puzzle.chest", "solved"));
        assertThrows(StateChangeRejectedException.class,
                () -> service.applyServerChange(session.id(), "alice", "bad key!", "solved"));
        assertEquals(0, service.snapshot(session.id(), "alice").version());
    }

    @Test
    void concurrentChangesGetUniqueConsecutiveVersionsAndArriveInOrder() throws Exception {
        final int attempts = 30;
        final List<SharedStateUpdate> received = java.util.Collections.synchronizedList(new ArrayList<>());
        channel.subscribe(session.id(), received::add);
        final ExecutorService pool = Executors.newFixedThreadPool(attempts);
        final CountDownLatch start = new CountDownLatch(1);
        final List<Future<SharedStateUpdate>> results = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            final String key = "k" + i;
            results.add(pool.submit(() -> {
                start.await();
                return service.applyChange(session.id(), "alice", key, "v");
            }));
        }
        start.countDown();
        final Set<Long> versions = new HashSet<>();
        for (final Future<SharedStateUpdate> result : results) {
            versions.add(result.get().version());
        }
        pool.shutdown();

        assertEquals(attempts, versions.size());
        assertEquals(attempts, service.snapshot(session.id(), "alice").version());
        for (int i = 0; i < attempts; i++) {
            assertEquals(i + 1, received.get(i).version());
        }
    }
}
