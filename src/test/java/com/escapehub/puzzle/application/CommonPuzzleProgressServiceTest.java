package com.escapehub.puzzle.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.escapehub.gameplay.application.CreateGameSessionService;
import com.escapehub.gameplay.application.JoinGameSessionService;
import com.escapehub.gameplay.domain.Game;
import com.escapehub.gameplay.domain.GameSession;
import com.escapehub.gameplay.domain.ItemType;
import com.escapehub.gameplay.infrastructure.InMemoryGameRepository;
import com.escapehub.gameplay.infrastructure.InMemoryGameSessionRepository;
import com.escapehub.gameplay.infrastructure.InMemorySessionParticipantsRepository;
import com.escapehub.puzzle.domain.CommonPuzzleProgressView;
import com.escapehub.puzzle.domain.PuzzleDefinition;
import java.util.List;
import com.escapehub.puzzle.domain.PuzzleStatus;
import com.escapehub.puzzle.infrastructure.InMemoryPuzzleCatalog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommonPuzzleProgressServiceTest {
    private InMemoryGameRepository games;
    private InMemorySessionParticipantsRepository participants;
    private CreateGameSessionService creator;
    private JoinGameSessionService joiner;
    private CommonPuzzleProgressService service;
    private InMemoryGameSessionRepository sessions;
    private GameSession session;

    @BeforeEach
    void setUp() {
        games = new InMemoryGameRepository();
        sessions = new InMemoryGameSessionRepository();
        participants = new InMemorySessionParticipantsRepository();
        creator = new CreateGameSessionService(sessions);
        joiner = new JoinGameSessionService(sessions, participants);
        service = new CommonPuzzleProgressService(games, sessions, participants, new InMemoryPuzzleCatalog());
        session = creator.createSession("player1");
    }

    @Test
    void returnsUnsolvedProgressForNewSession() {
        CommonPuzzleProgressView progress = service.getCommonProgress(session.id(), "player1");
        assertEquals(1, progress.totalPuzzles());
        assertEquals(0, progress.solvedCount());
        assertEquals(progress.totalPuzzles(), progress.remainingCount());
        assertEquals(PuzzleStatus.UNSOLVED, progress.puzzles().get(0).status());
    }

    @Test
    void readsSolvedPuzzleFromAuthoritativeGameStateForEveryParticipant() {
        Game game = new Game(session.id());
        game.completePuzzle("chest");
        game.collectItem(ItemType.LAB_KEY);
        game.unlockExit();
        games.save(game);
        joiner.joinSession(session.joinCode(), "player2");

        CommonPuzzleProgressView progress = service.getCommonProgress(session.id(), "player2");
        assertEquals(1, progress.solvedCount());
        assertEquals(100, progress.completionPercentage());
        assertEquals("chest", progress.puzzles().get(0).puzzleId());
        assertEquals("Σεντούκι", progress.puzzles().get(0).title());
        assertTrue(progress.puzzles().get(0).solved());
    }

    @Test
    void creatorRetainsAccessEvenIfNotListedAsParticipant() {
        joiner.joinSession(session.joinCode(), "player2");
        var tracking = participants.findBySessionId(session.id()).orElseThrow();
        tracking.removePlayer("player1");
        participants.save(tracking);

        assertEquals(1, service.getCommonProgress(session.id(), "PLAYER1").totalPuzzles());
    }

    @Test
    void rejectsNonParticipantsAndInactiveSessions() {
        assertThrows(SecurityException.class, () -> service.getCommonProgress(session.id(), "intruder"));
        joiner.joinSession(session.joinCode(), "player2");
        var tracking = participants.findBySessionId(session.id()).orElseThrow();
        tracking.deactivate();
        participants.save(tracking);
        assertThrows(IllegalStateException.class, () -> service.getCommonProgress(session.id(), "player2"));
    }

    @Test
    void rejectsNonParticipantBeforeCheckingWhetherSessionIsActive() {
        joiner.joinSession(session.joinCode(), "player2");
        var tracking = participants.findBySessionId(session.id()).orElseThrow();
        tracking.deactivate();
        participants.save(tracking);

        assertThrows(SecurityException.class,
                () -> service.getCommonProgress(session.id(), "intruder"));
    }

    @Test
    void returnsZeroPercentWhenCatalogIsEmpty() {
        CommonPuzzleProgressService emptyCatalogService = new CommonPuzzleProgressService(
                games,
                sessions,
                participants,
                new PuzzleCatalog() {
                    @Override
                    public List<PuzzleDefinition> findAll() {
                        return List.of();
                    }

                    @Override
                    public java.util.Optional<PuzzleDefinition> findById(final String id) {
                        return java.util.Optional.empty();
                    }
                });

        CommonPuzzleProgressView progress =
                emptyCatalogService.getCommonProgress(session.id(), "player1");

        assertEquals(0, progress.totalPuzzles());
        assertEquals(0, progress.solvedCount());
        assertEquals(0, progress.remainingCount());
        assertEquals(0, progress.completionPercentage());
    }
}
