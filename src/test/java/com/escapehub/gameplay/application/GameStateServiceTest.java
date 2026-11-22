package com.escapehub.gameplay.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.escapehub.gameplay.domain.Game;
import com.escapehub.gameplay.domain.GameSession;
import com.escapehub.gameplay.domain.GameState;
import com.escapehub.gameplay.domain.GameStatus;
import com.escapehub.gameplay.infrastructure.InMemoryGameRepository;
import com.escapehub.gameplay.infrastructure.InMemoryGameSessionRepository;
import com.escapehub.gameplay.infrastructure.InMemorySessionParticipantsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GameStateServiceTest {

    private CreateGameSessionService createService;
    private JoinGameSessionService joinService;
    private GameStateService gameService;

    @BeforeEach
    void setUp() {
        final InMemoryGameSessionRepository sessions = new InMemoryGameSessionRepository();
        final InMemorySessionParticipantsRepository participants =
                new InMemorySessionParticipantsRepository();
        createService = new CreateGameSessionService(sessions);
        joinService = new JoinGameSessionService(sessions, participants);
        gameService = new GameStateService(sessions, participants, new InMemoryGameRepository());
    }

    @Test
    void firstEntryInitializesGameInInitialRoomWithDefaultState() {
        final GameSession session = createService.createSession("alice");

        final GameState state = gameService.enterGame(session.joinCode(), "alice");

        assertEquals(session.id(), state.sessionId());
        assertEquals(Game.INITIAL_ROOM_ID, state.currentRoomId());
        assertEquals(GameStatus.IN_PROGRESS, state.status());
        assertTrue(state.attributes().isEmpty());
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
        assertThrows(IllegalArgumentException.class, () -> gameService.enterGame("ZZZZZZ", "alice"));
        assertThrows(IllegalArgumentException.class, () -> gameService.enterGame(" ", "alice"));
    }

    @Test
    void snapshotsDoNotExposeLiveState() {
        final GameSession session = createService.createSession("alice");
        final GameState before = gameService.enterGame(session.joinCode(), "alice");

        gameService.updateAttribute(session.joinCode(), "alice", "k", "v");

        assertTrue(before.attributes().isEmpty());
    }
}