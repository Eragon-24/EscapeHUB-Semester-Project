package com.escapehub.gameplay.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.escapehub.gameplay.domain.GameSession;
import com.escapehub.gameplay.domain.GameState;
import com.escapehub.gameplay.domain.SessionParticipants;
import com.escapehub.gameplay.domain.SharedStateUpdate;
import com.escapehub.gameplay.infrastructure.InMemoryGameSessionRepository;
import com.escapehub.gameplay.infrastructure.InMemorySessionParticipantsRepository;
import com.escapehub.gameplay.infrastructure.InMemorySessionUpdateChannel;
import com.escapehub.gameplay.infrastructure.InMemoryGameRepository;
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
    private InMemoryGameRepository games;
    private GameStateService gameStates;
    private InMemorySessionUpdateChannel channel;
    private List<StateChangeRule> rules;
    private SharedStateService service;
    private RoomItemCatalog itemCatalog;
    private JoinGameSessionService joinService;
    private GameSession session;

    @BeforeEach
    void setUp() {
        sessions = new InMemoryGameSessionRepository();
        participants = new InMemorySessionParticipantsRepository();
        games = new InMemoryGameRepository();
        channel = new InMemorySessionUpdateChannel();
        gameStates = new GameStateService(sessions, participants, games, channel, clock);
        rules = new ArrayList<>();
        itemCatalog = new RoomItemCatalog(List.of("lab-notebook", "silver-key"));
        service = new SharedStateService(sessions, participants, gameStates, rules, itemCatalog);
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
        assertEquals("open", service.snapshot(session.id(), "bob").attributes().get("door"));
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

        assertThrows(AccessDeniedException.class,
                () -> service.applyChange(session.id(), "mallory", "door", "open"));
        assertThrows(AccessDeniedException.class,
                () -> service.snapshot(session.id(), "mallory"));
        assertThrows(AccessDeniedException.class,
                () -> service.requireParticipant(session.id(), " "));
        assertTrue(received.isEmpty());
        assertEquals(0, service.snapshot(session.id(), "alice").version());
    }

    @Test
    void creatorMustRemainInAnExistingParticipantRecord() {
        participants.save(new SessionParticipants(session.id(), "bob"));

        assertThrows(AccessDeniedException.class,
                () -> service.snapshot(session.id(), "alice"));
        assertThrows(SecurityException.class,
                () -> gameStates.enterGame(session.joinCode(), "alice"));
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
        assertTrue(service.snapshot(other.id(), "dave").attributes().isEmpty());
    }

    @Test
    void participantOfOneSessionCannotChangeAnotherSession() {
        final GameSession other = new CreateGameSessionService(sessions).createSession("dave");

        assertThrows(AccessDeniedException.class,
                () -> service.applyChange(other.id(), "alice", "door", "open"));
        assertTrue(service.snapshot(other.id(), "dave").attributes().isEmpty());
    }

    @Test
    void changeRefusedByARuleIsNotAppliedNorBroadcast() {
        rules.add((sessionId, user, key, value) -> {
            if ("exit".equals(key)) {
                throw new StateChangeRejectedException("Η έξοδος είναι κλειδωμένη.");
            }
        });
        service = new SharedStateService(sessions, participants, gameStates, rules, itemCatalog);
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

        assertThrows(StateChangeRejectedException.class,
                () -> service.applyChange(session.id(), "alice", null, "v"));
        assertThrows(StateChangeRejectedException.class,
                () -> service.applyChange(session.id(), "alice", "", "v"));
        assertThrows(StateChangeRejectedException.class,
                () -> service.applyChange(session.id(), "alice", "bad key!", "v"));
        assertThrows(StateChangeRejectedException.class,
                () -> service.applyChange(session.id(), "alice", "game.currentRoomId", "vault"));
        assertThrows(StateChangeRejectedException.class,
                () -> service.applyChange(session.id(), "alice", "k", null));
        assertThrows(StateChangeRejectedException.class,
                () -> service.applyChange(session.id(), "alice", "k", tooLong));
        assertEquals(0, service.snapshot(session.id(), "alice").version());
    }

    @Test
    void inactiveSessionRejectsChanges() {
        final SessionParticipants current = participants.findBySessionId(session.id()).orElseThrow();
        current.deactivate();
        participants.save(current);

        assertThrows(IllegalStateException.class,
                () -> service.applyChange(session.id(), "alice", "door", "open"));
        assertThrows(IllegalStateException.class,
                () -> service.snapshot(session.id(), "alice"));
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
    void acceptedChangeIsStoredInTheSessionsGameOfUs013() {
        service.applyChange(session.id(), "alice", "door", "open");

        final GameState game = games.findBySessionId(session.id()).orElseThrow().snapshot();

        assertEquals("open", game.attributes().get("door"));
        assertEquals(1, game.version());
        assertEquals(session.id(), game.sessionId());
        assertEquals(game, gameStates.getState(session.joinCode(), "bob").orElseThrow());
    }

    @Test
    void firstSnapshotInitializesTheGameWithItsInitialState() {
        assertTrue(games.findBySessionId(session.id()).isEmpty());

        final GameState initial = service.snapshot(session.id(), "alice");

        assertEquals(0, initial.version());
        assertEquals(com.escapehub.gameplay.domain.Game.INITIAL_ROOM_ID, initial.currentRoomId());
        assertTrue(initial.attributes().isEmpty());
        assertTrue(games.findBySessionId(session.id()).isPresent());
    }

    @Test
    void changesMadeThroughTheGameStateServiceAreVisibleAndPublished() {
        final List<SharedStateUpdate> received = listenTo(session.id());
        gameStates.enterGame(session.joinCode(), "alice");
        gameStates.updateAttribute(session.joinCode(), "alice", "lamp", "on");
        gameStates.changeRoom(session.joinCode(), "alice", "vault");

        final GameState snapshot = service.snapshot(session.id(), "bob");

        assertEquals("on", snapshot.attributes().get("lamp"));
        assertEquals("vault", snapshot.currentRoomId());
        assertEquals(2, snapshot.version());
        assertEquals(2, received.size());
        assertEquals("lamp", received.get(0).key());
        assertEquals("game.currentRoomId", received.get(1).key());
        assertEquals(2, received.get(1).version());
    }

    @Test
    void reconnectingParticipantGetsTheCurrentStateInASnapshot() {
        service.applyChange(session.id(), "alice", "door", "open");
        service.applyChange(session.id(), "alice", "lamp", "on");

        final GameState snapshot = service.snapshot(session.id(), "bob");

        assertEquals(2, snapshot.version());
        assertEquals("open", snapshot.attributes().get("door"));
        assertEquals("on", snapshot.attributes().get("lamp"));
    }

    @Test
    void pickupStoresInventoryAndPublishesToTheSession() {
        final List<SharedStateUpdate> received = listenTo(session.id());

        final SharedStateUpdate opened = service.interact(session.id(), "alice", "lab-notebook", "pickup");

        assertEquals("inventory.lab-notebook", opened.key());
        assertEquals("true", opened.value());
        assertEquals(List.of(opened), received);
        assertEquals("true", service.snapshot(session.id(), "alice").attributes().get("inventory.lab-notebook"));
    }

    @Test
    void oneTimePickupCannotBeRepeated() {
        service.interact(session.id(), "alice", "lab-notebook", "pickup");
        final List<SharedStateUpdate> received = listenTo(session.id());

        assertThrows(StateChangeRejectedException.class,
                () -> service.interact(session.id(), "bob", "lab-notebook", "pickup"));
        assertThrows(StateChangeRejectedException.class,
                () -> service.interact(session.id(), "bob", "lab-notebook", "pickup"));

        assertTrue(received.isEmpty());
        assertEquals("true",
                service.snapshot(session.id(), "bob").attributes().get("inventory.lab-notebook"));
    }

    @Test
    void invalidObjectRoomAndActionAreRejectedWithoutMutation() {
        assertThrows(StateChangeRejectedException.class,
                () -> service.interact(session.id(), "alice", "missing-object", "pickup"));
        assertThrows(StateChangeRejectedException.class,
                () -> service.interact(session.id(), "alice", "lab-notebook", "open"));
        gameStates.changeRoom(session.joinCode(), "alice", "vault");
        final long versionAfterRoomChange = service.snapshot(session.id(), "alice").version();

        assertThrows(StateChangeRejectedException.class,
                () -> service.interact(session.id(), "alice", "lab-notebook", "pickup"));

        assertEquals(versionAfterRoomChange, service.snapshot(session.id(), "alice").version());
        assertTrue(service.snapshot(session.id(), "alice").attributes().isEmpty());
    }

    @Test
    void objectUpdatesAreSessionScoped() {
        final GameSession other = new CreateGameSessionService(sessions).createSession("dave");
        final List<SharedStateUpdate> otherView = listenTo(other.id());
        final List<SharedStateUpdate> sameSessionView = listenTo(session.id());

        service.interact(session.id(), "alice", "lab-notebook", "pickup");

        assertEquals(1, sameSessionView.size());
        assertTrue(otherView.isEmpty());
        assertTrue(service.snapshot(other.id(), "dave").attributes().isEmpty());
    }

    @Test
    void concurrentCollectionAllowsExactlyOneSuccessfulTransition() throws Exception {
        final ExecutorService pool = Executors.newFixedThreadPool(2);
        final CountDownLatch start = new CountDownLatch(1);
        final List<Future<Boolean>> attempts = new ArrayList<>();
        for (final String player : List.of("alice", "bob")) {
            attempts.add(pool.submit(() -> {
                start.await();
                try {
                    service.interact(session.id(), player, "lab-notebook", "pickup");
                    return true;
                } catch (StateChangeRejectedException rejected) {
                    return false;
                }
            }));
        }
        start.countDown();
        final long successes = attempts.stream().filter(future -> {
            try {
                return future.get();
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        }).count();
        pool.shutdown();

        assertEquals(1, successes);
        assertEquals("true",
                service.snapshot(session.id(), "alice").attributes().get("inventory.lab-notebook"));
        assertEquals(1, service.snapshot(session.id(), "alice").version());
    }

    @Test
    void genericPickupAddsAnyConfiguredItemToSharedInventory() {
        final List<SharedStateUpdate> received = listenTo(session.id());

        final SharedStateUpdate update = service.interact(session.id(), "bob", "silver-key", "pickup");

        assertEquals("inventory.silver-key", update.key());
        assertEquals("true", service.snapshot(session.id(), "alice").attributes().get("inventory.silver-key"));
        assertEquals(List.of(update), received);
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
