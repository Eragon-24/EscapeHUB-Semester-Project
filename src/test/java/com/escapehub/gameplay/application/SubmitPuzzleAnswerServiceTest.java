package com.escapehub.gameplay.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.escapehub.gameplay.application.PuzzleSubmissionResult.Status;
import com.escapehub.gameplay.domain.Game;
import com.escapehub.gameplay.domain.GameSession;
import com.escapehub.gameplay.domain.GameState;
import com.escapehub.gameplay.domain.ItemType;
import com.escapehub.gameplay.domain.SessionParticipants;
import com.escapehub.gameplay.domain.SharedStateUpdate;
import com.escapehub.gameplay.infrastructure.InMemoryGameSessionRepository;
import com.escapehub.gameplay.infrastructure.InMemorySessionParticipantsRepository;
import com.escapehub.gameplay.infrastructure.InMemorySessionUpdateChannel;
import com.escapehub.gameplay.infrastructure.InMemoryGameRepository;
import com.escapehub.puzzle.infrastructure.InMemoryPuzzleCatalog;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class SubmitPuzzleAnswerServiceTest {

    private static final String CORRECT = "483920";

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-22T10:00:00Z"), ZoneOffset.UTC);

    private InMemoryGameSessionRepository sessions;
    private InMemorySessionParticipantsRepository participants;
    private InMemoryGameRepository games;
    private InMemorySessionUpdateChannel channel;
    private GameStateService gameStates;
    private SharedStateService sharedState;
    private SubmitPuzzleAnswerService service;
    private GameSession session;

    @BeforeEach
    void setUp() {
        sessions = new InMemoryGameSessionRepository();
        participants = new InMemorySessionParticipantsRepository();
        games = new InMemoryGameRepository();
        channel = new InMemorySessionUpdateChannel();
        gameStates = new GameStateService(sessions, participants, games, channel, clock);
        sharedState = new SharedStateService(
                sessions, participants, gameStates, List.of(),
                new RoomItemCatalog(List.of("lab-notebook")));
        service = new SubmitPuzzleAnswerService(sessions, sharedState, gameStates, new InMemoryPuzzleCatalog());
        session = new CreateGameSessionService(sessions).createSession("alice");
        new JoinGameSessionService(sessions, participants).joinSession(session.joinCode(), "bob");
    }

    private boolean isSolved(final UUID sessionId, final String participant) {
        return games.findBySessionId(sessionId)
                .map(game -> game.snapshot().completedPuzzleIds().contains("chest")).orElse(false);
    }

    private List<SharedStateUpdate> listenTo(final UUID sessionId) {
        final List<SharedStateUpdate> received = Collections.synchronizedList(new ArrayList<>());
        channel.subscribe(sessionId, received::add);
        return received;
    }

    @Test
    void correctAnswerIsAcceptedAndSolvesThePuzzleForTheSession() {
        final PuzzleSubmissionResult result = service.submit(session.id(), "alice", "chest", CORRECT);

        assertEquals(Status.ACCEPTED, result.status());
        assertEquals("chest", result.puzzleId());
        assertTrue(isSolved(session.id(), "alice"));
    }

    @Test
    void correctAnswerIsPublishedToTheOtherParticipantsAsSharedProgress() {
        final List<SharedStateUpdate> bobsView = listenTo(session.id());

        service.submit(session.id(), "alice", "chest", CORRECT);

        assertEquals(List.of("progress.puzzles.chest", "progress.items.LAB_KEY", "game.exitUnlocked"),
                bobsView.stream().map(SharedStateUpdate::key).toList());
        assertEquals(List.of(1L, 2L, 3L), bobsView.stream().map(SharedStateUpdate::version).toList());
        assertEquals("alice", bobsView.get(0).author());
    }

    @Test
    void correctAnswerGrantsTheKeyAndUnlocksTheExitForTheWholeSession() {
        service.submit(session.id(), "alice", "chest", CORRECT);

        final GameState seenByBob = sharedState.snapshot(session.id(), "bob");
        assertEquals(Set.of("chest"), seenByBob.completedPuzzleIds());
        assertEquals(Set.of(ItemType.LAB_KEY), seenByBob.collectedItems());
        assertTrue(seenByBob.exitUnlocked());
    }

    @Test
    void wrongAnswerLeavesTheExitLocked() {
        service.submit(session.id(), "alice", "chest", "000000");

        final GameState state = sharedState.snapshot(session.id(), "alice");
        assertFalse(state.exitUnlocked());
        assertTrue(state.collectedItems().isEmpty());
    }

    @Test
    void answerIsTrimmedBeforeChecking() {
        assertEquals(Status.ACCEPTED, service.submit(session.id(), "bob", "chest", "  483920  ").status());
    }

    @Test
    void wrongAnswerIsRejectedWithFeedbackAndChangesNothing() {
        final List<SharedStateUpdate> received = listenTo(session.id());

        final PuzzleSubmissionResult result = service.submit(session.id(), "alice", "chest", "111111");

        assertEquals(Status.REJECTED, result.status());
        assertFalse(result.message().isBlank());
        assertFalse(isSolved(session.id(), "alice"));
        assertTrue(received.isEmpty());
        assertEquals(0, sharedState.snapshot(session.id(), "alice").version());
    }

    @Test
    void blankOrMissingAnswersAreRejected() {
        assertEquals(Status.REJECTED, service.submit(session.id(), "alice", "chest", "").status());
        assertEquals(Status.REJECTED, service.submit(session.id(), "alice", "chest", "   ").status());
        assertEquals(Status.REJECTED, service.submit(session.id(), "alice", "chest", null).status());
        assertFalse(isSolved(session.id(), "alice"));
    }

    @Test
    void wrongAnswerAfterAWrongOneStillAllowsTheCorrectOne() {
        service.submit(session.id(), "alice", "chest", "000000");

        assertEquals(Status.ACCEPTED, service.submit(session.id(), "alice", "chest", CORRECT).status());
    }

    @Test
    void solvedPuzzleCannotBeSolvedAgainAndItsEffectIsNotDuplicated() {
        final List<SharedStateUpdate> received = listenTo(session.id());
        service.submit(session.id(), "alice", "chest", CORRECT);

        final PuzzleSubmissionResult again = service.submit(session.id(), "bob", "chest", CORRECT);

        assertEquals(Status.ALREADY_SOLVED, again.status());
        assertEquals(3, received.size());
        assertEquals(3, sharedState.snapshot(session.id(), "alice").version());
        assertEquals("alice", received.get(0).author());
    }

    @Test
    void wrongAnswerAfterSolvedDoesNotUndoTheSolution() {
        service.submit(session.id(), "alice", "chest", CORRECT);

        assertEquals(Status.ALREADY_SOLVED, service.submit(session.id(), "bob", "chest", "000000").status());
        assertTrue(isSolved(session.id(), "alice"));
    }

    @Test
    void nonParticipantCannotSubmitAndNothingChanges() {
        final List<SharedStateUpdate> received = listenTo(session.id());

        assertThrows(AccessDeniedException.class, () -> service.submit(session.id(), "mallory", "chest", CORRECT));

        assertFalse(isSolved(session.id(), "alice"));
        assertTrue(received.isEmpty());
    }

    @Test
    void unknownSessionAndUnknownPuzzleAreReported() {
        assertThrows(NoSuchElementException.class,
                () -> service.submit(UUID.randomUUID(), "alice", "chest", CORRECT));
        assertThrows(NoSuchElementException.class, () -> service.submit(session.id(), "alice", "vault", CORRECT));
    }

    @Test
    void inactiveSessionRejectsSubmissions() {
        final SessionParticipants current = participants.findBySessionId(session.id()).orElseThrow();
        current.deactivate();
        participants.save(current);

        assertThrows(IllegalStateException.class, () -> service.submit(session.id(), "alice", "chest", CORRECT));
        final boolean solvedInGame = games.findBySessionId(session.id())
                .map(game -> game.snapshot().completedPuzzleIds().contains("chest")).orElse(false);
        assertFalse(solvedInGame);
    }

    @Test
    void submissionsAndResultsStayInsideTheirOwnSession() {
        final GameSession other = new CreateGameSessionService(sessions).createSession("dave");
        final List<SharedStateUpdate> otherSessionView = listenTo(other.id());

        service.submit(session.id(), "alice", "chest", CORRECT);

        assertFalse(isSolved(other.id(), "dave"));
        assertTrue(otherSessionView.isEmpty());
        assertEquals(Status.ACCEPTED, service.submit(other.id(), "dave", "chest", CORRECT).status());
        assertThrows(AccessDeniedException.class, () -> service.submit(other.id(), "alice", "chest", CORRECT));
    }

    @Test
    void simultaneousCorrectAnswersSolveThePuzzleExactlyOnce() throws Exception {
        final int attempts = 20;
        final List<SharedStateUpdate> received = listenTo(session.id());
        final ExecutorService pool = Executors.newFixedThreadPool(attempts);
        final CountDownLatch start = new CountDownLatch(1);
        final List<Future<PuzzleSubmissionResult>> results = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            final String player = i % 2 == 0 ? "alice" : "bob";
            results.add(pool.submit(() -> {
                start.await();
                return service.submit(session.id(), player, "chest", CORRECT);
            }));
        }
        start.countDown();
        int accepted = 0;
        int alreadySolved = 0;
        for (final Future<PuzzleSubmissionResult> result : results) {
            final Status status = result.get().status();
            if (status == Status.ACCEPTED) {
                accepted++;
            } else if (status == Status.ALREADY_SOLVED) {
                alreadySolved++;
            }
        }
        pool.shutdown();

        assertEquals(1, accepted);
        assertEquals(attempts - 1, alreadySolved);
        assertEquals(3, received.size());
    }

    @Test
    void solutionIsSavedWhenTheRepositoryReturnsCopiesLikeADatabase() {
        final GameRepository database = new CopyingGameRepository();
        final GameStateService persistentStates =
                new GameStateService(sessions, participants, database, channel, clock);
        final SubmitPuzzleAnswerService persistent = new SubmitPuzzleAnswerService(sessions,
                new SharedStateService(
                        sessions, participants, persistentStates, List.of(),
                        new RoomItemCatalog(List.of("lab-notebook"))),
                persistentStates, new InMemoryPuzzleCatalog());

        assertEquals(Status.ACCEPTED, persistent.submit(session.id(), "alice", "chest", CORRECT).status());
        assertEquals(Status.ALREADY_SOLVED, persistent.submit(session.id(), "bob", "chest", CORRECT).status());

        final var saved = database.findBySessionId(session.id()).orElseThrow().snapshot();
        assertEquals(Set.of("chest"), saved.completedPuzzleIds());
        assertEquals(Set.of(ItemType.LAB_KEY), saved.collectedItems());
        assertTrue(saved.exitUnlocked());
        assertEquals(3, saved.version());
    }

    /** Like the PostgreSQL repository: every read returns a new object, so only saved changes survive. */
    private static final class CopyingGameRepository implements GameRepository {

        private final Map<UUID, GameState> rows = new ConcurrentHashMap<>();

        @Override
        public Game save(final Game game) {
            rows.put(game.getSessionId(), game.snapshot());
            return game;
        }

        @Override
        public Optional<Game> findBySessionId(final UUID sessionId) {
            return Optional.ofNullable(rows.get(sessionId)).map(Game::restore);
        }
    }
}
