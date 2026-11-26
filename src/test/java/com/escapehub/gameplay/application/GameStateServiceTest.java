package com.escapehub.gameplay.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.escapehub.gameplay.domain.Game;
import com.escapehub.gameplay.domain.GameSession;
import com.escapehub.gameplay.domain.GameState;
import com.escapehub.gameplay.domain.GameStatus;
import com.escapehub.gameplay.domain.ItemType;
import com.escapehub.gameplay.domain.SharedStateUpdate;
import com.escapehub.gameplay.infrastructure.InMemoryGameRepository;
import com.escapehub.gameplay.infrastructure.InMemorySessionUpdateChannel;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import com.escapehub.gameplay.infrastructure.InMemoryGameSessionRepository;
import com.escapehub.gameplay.infrastructure.InMemorySessionParticipantsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GameStateServiceTest {

    private CreateGameSessionService createService;
    private JoinGameSessionService joinService;
    private GameStateService gameService;
    private InMemorySessionUpdateChannel updateChannel;

    @BeforeEach
    void setUp() {
        final InMemoryGameSessionRepository sessions = new InMemoryGameSessionRepository();
        final InMemorySessionParticipantsRepository participants =
                new InMemorySessionParticipantsRepository();
        createService = new CreateGameSessionService(sessions);
        joinService = new JoinGameSessionService(sessions, participants);
        updateChannel = new InMemorySessionUpdateChannel();
        gameService = new GameStateService(
                sessions, participants, new InMemoryGameRepository(), updateChannel, Clock.systemUTC());
    }

    @Test
    void firstEntryInitializesGameInInitialRoomWithDefaultState() {
        final GameSession session = createService.createSession("alice");

        final GameState state = gameService.enterGame(session.joinCode(), "alice");

        assertEquals(session.id(), state.sessionId());
        assertEquals(Game.INITIAL_ROOM_ID, state.currentRoomId());
        assertEquals(GameStatus.IN_PROGRESS, state.status());
        assertTrue(state.attributes().isEmpty());
        assertTrue(state.completedPuzzleIds().isEmpty());
        assertTrue(state.collectedItems().isEmpty());
        assertFalse(state.exitUnlocked());
    }

    @Test
    void joiningParticipantReceivesExistingStateWithoutReset() {
        final GameSession session = createService.createSession("alice");
        gameService.enterGame(session.joinCode(), "alice");
        gameService.changeRoom(session.joinCode(), "alice", "vault");
        gameService.updateAttribute(session.joinCode(), "alice", "chestOpen", "true");
        joinService.joinSession(session.joinCode(), "bob");

        final GameState state = gameService.enterGame(session.joinCode(), "bob");

        assertEquals("vault", state.currentRoomId());
        assertEquals("true", state.attributes().get("chestOpen"));
        assertEquals(2, state.version());
    }

    @Test
    void sessionsHaveIsolatedGames() {
        final GameSession first = createService.createSession("alice");
        final GameSession second = createService.createSession("carol");
        gameService.enterGame(first.joinCode(), "alice");
        gameService.enterGame(second.joinCode(), "carol");

        gameService.changeRoom(first.joinCode(), "alice", "vault");

        assertEquals("vault", gameService.enterGame(first.joinCode(), "alice").currentRoomId());
        final GameState other = gameService.enterGame(second.joinCode(), "carol");
        assertEquals(Game.INITIAL_ROOM_ID, other.currentRoomId());
        assertNotEquals(first.id(), other.sessionId());
    }

    @Test
    void nonParticipantCannotEnterOrReadGame() {
        final GameSession session = createService.createSession("alice");

        assertThrows(SecurityException.class,
                () -> gameService.enterGame(session.joinCode(), "mallory"));
        assertThrows(SecurityException.class,
                () -> gameService.getState(session.joinCode(), "mallory"));
    }

    @Test
    void getStateIsEmptyBeforeEntryAndUpdateRequiresGame() {
        final GameSession session = createService.createSession("alice");

        assertTrue(gameService.getState(session.joinCode(), "alice").isEmpty());
        assertThrows(IllegalStateException.class,
                () -> gameService.changeRoom(session.joinCode(), "alice", "vault"));
    }

    @Test
    void rejectsUnknownOrBlankJoinCode() {
        assertThrows(GameSessionNotFoundException.class,
                () -> gameService.enterGame("ZZZZZZ", "alice"));
        assertThrows(IllegalArgumentException.class, () -> gameService.enterGame(" ", "alice"));
    }

    @Test
    void snapshotsDoNotExposeLiveState() {
        final GameSession session = createService.createSession("alice");
        final GameState before = gameService.enterGame(session.joinCode(), "alice");

        gameService.updateAttribute(session.joinCode(), "alice", "k", "v");

        assertTrue(before.attributes().isEmpty());
    }
    @Test
    void directGameMutationsPublishVersionedUpdatesToTheSameSession() {
        final GameSession session = createService.createSession("alice");
        gameService.enterGame(session.joinCode(), "alice");
        final List<SharedStateUpdate> received = new ArrayList<>();
        updateChannel.subscribe(session.id(), received::add);

        gameService.changeRoom(session.joinCode(), "alice", "vault");
        gameService.updateAttribute(session.joinCode(), "alice", "doorOpen", "true");

        assertEquals(2, received.size());
        assertEquals("game.currentRoomId", received.get(0).key());
        assertEquals("vault", received.get(0).value());
        assertEquals(1, received.get(0).version());
        assertEquals("doorOpen", received.get(1).key());
        assertEquals("true", received.get(1).value());
        assertEquals(2, received.get(1).version());
    }

    @Test
    void completingThePuzzleUnlocksTheExitAndPublishesOneTimeProgress() {
        final GameSession session = createService.createSession("alice");
        gameService.enterGame(session.joinCode(), "alice");
        final List<SharedStateUpdate> received = new ArrayList<>();
        updateChannel.subscribe(session.id(), received::add);

        final GameState solved = gameService
                .completePuzzle(session.joinCode(), "alice", "chest", java.util.Set.of(ItemType.LAB_KEY))
                .orElseThrow();

        assertTrue(solved.exitUnlocked());
        assertEquals(java.util.Set.of("chest"), solved.completedPuzzleIds());
        assertEquals(java.util.Set.of(ItemType.LAB_KEY), solved.collectedItems());
        assertEquals(List.of(
                "progress.puzzles.chest",
                "progress.items.LAB_KEY",
                "game.exitUnlocked"), received.stream().map(SharedStateUpdate::key).toList());
        assertEquals(List.of(1L, 2L, 3L), received.stream().map(SharedStateUpdate::version).toList());

        joinService.joinSession(session.joinCode(), "bob");
        assertTrue(gameService.enterGame(session.joinCode(), "bob").exitUnlocked());
        assertTrue(gameService
                .completePuzzle(session.joinCode(), "alice", "chest", java.util.Set.of(ItemType.LAB_KEY))
                .isEmpty());
        assertEquals(3, gameService.enterGame(session.joinCode(), "alice").version());
        assertEquals(3, received.size());
    }

    @Test
    void completingAPuzzleWithoutTheExitRequirementsKeepsTheExitLocked() {
        final GameSession session = createService.createSession("alice");
        gameService.enterGame(session.joinCode(), "alice");

        final GameState state = gameService
                .completePuzzle(session.joinCode(), "alice", "other", java.util.Set.of())
                .orElseThrow();

        assertFalse(state.exitUnlocked());
    }

    @Test
    void puzzleProgressIsIsolatedToTheSession() {
        final GameSession first = createService.createSession("alice");
        final GameSession second = createService.createSession("carol");
        gameService.enterGame(first.joinCode(), "alice");
        gameService.enterGame(second.joinCode(), "carol");

        gameService.completePuzzle(first.joinCode(), "alice", "chest", java.util.Set.of(ItemType.LAB_KEY));

        assertTrue(gameService.enterGame(first.joinCode(), "alice").exitUnlocked());
        assertFalse(gameService.enterGame(second.joinCode(), "carol").exitUnlocked());
    }
}
