package com.escapehub.gameplay.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GameSessionServiceTest {

    private GameSessionService service;

    @BeforeEach
    void setUp() {
        service = new GameSessionService();
    }

    @Test
    void shouldCreateSeparatePlayersInTheSameRoom() {
        final String firstPlayer = service.addPlayer();
        final String secondPlayer = service.addPlayer();

        final GameSessionService.GameSnapshot firstView = service.snapshotFor(firstPlayer);
        final GameSessionService.GameSnapshot secondView = service.snapshotFor(secondPlayer);

        assertNotEquals(firstPlayer, secondPlayer);
        assertEquals(2, firstView.players().size());
        assertEquals(2, secondView.players().size());
        assertEquals(firstPlayer, firstView.you());
        assertEquals(secondPlayer, secondView.you());
    }

    @Test
    void shouldMoveOnlyThePlayerWhoSentTheInput() {
        final String firstPlayer = service.addPlayer();
        final String secondPlayer = service.addPlayer();
        final GameSessionService.PlayerSnapshot secondInitial =
                findPlayer(service.snapshotFor(secondPlayer), secondPlayer);

        service.movePlayer(firstPlayer, 1, 0);

        final GameSessionService.GameSnapshot state = service.snapshotFor(secondPlayer);
        final GameSessionService.PlayerSnapshot movedPlayer = findPlayer(state, firstPlayer);
        final GameSessionService.PlayerSnapshot otherPlayer = findPlayer(state, secondPlayer);

        assertTrue(movedPlayer.x() > 360);
        assertEquals(secondInitial.x(), otherPlayer.x());
        assertEquals(secondInitial.y(), otherPlayer.y());
    }

    @Test
    void shouldNotAllowPlayersToOccupyTheSamePosition() {
        final String firstPlayer = service.addPlayer();
        final String secondPlayer = service.addPlayer();

        for (int step = 0; step < 4; step++) {
            service.movePlayer(firstPlayer, 1, 0);
        }
        for (int step = 0; step < 4; step++) {
            service.movePlayer(secondPlayer, -1, 0);
        }

        final GameSessionService.GameSnapshot state = service.snapshotFor(firstPlayer);
        final GameSessionService.PlayerSnapshot first = findPlayer(state, firstPlayer);
        final GameSessionService.PlayerSnapshot second = findPlayer(state, secondPlayer);

        assertTrue(Math.hypot(first.x() - second.x(), first.y() - second.y()) >= 30.0);
    }

    @Test
    void shouldShareNotebookDiscoveryWithEveryPlayer() {
        final String firstPlayer = service.addPlayer();
        final String secondPlayer = service.addPlayer();
        moveToNotebook(firstPlayer);

        service.interact(firstPlayer);

        final GameSessionService.GameSnapshot state = service.snapshotFor(secondPlayer);
        assertTrue(state.notebookFound());
        assertTrue(state.clueText().contains("4-8-3-9-2-0"));
    }

    @Test
    void shouldShareChestSolutionAndExitCompletionWithEveryPlayer() {
        final String firstPlayer = service.addPlayer();
        final String secondPlayer = service.addPlayer();

        moveToNotebook(firstPlayer);
        service.interact(firstPlayer);
        moveToChest(firstPlayer);

        final GameSessionService.GameSnapshot chestState =
                service.submitAnswer(firstPlayer, "483920");
        assertTrue(chestState.chestSolved());
        assertFalse(chestState.gameWon());

        moveToExit(firstPlayer);
        final GameSessionService.GameSnapshot exitState = service.interact(firstPlayer);
        assertTrue(exitState.gameWon());
        assertTrue(service.snapshotFor(secondPlayer).gameWon());
    }

    @Test
    void shouldRejectChestAnswerUntilNotebookHasBeenFound() {
        final String playerId = service.addPlayer();
        moveToChestWithoutClue(playerId);

        final GameSessionService.GameSnapshot state =
                service.submitAnswer(playerId, "483920");

        assertFalse(state.chestSolved());
        assertTrue(state.statusMessage().contains("σημειωματάριο"));
    }

    @Test
    void shouldRejectInvalidMovementDirections() {
        final String playerId = service.addPlayer();

        assertThrows(IllegalArgumentException.class,
                () -> service.movePlayer(playerId, 2, 0));
        assertThrows(IllegalArgumentException.class,
                () -> service.movePlayer(playerId, 0, 0));
    }

    @Test
    void shouldEnforceTheMaximumNumberOfConnectedPlayers() {
        for (int index = 0; index < 8; index++) {
            service.addPlayer();
        }

        assertThrows(IllegalStateException.class, service::addPlayer);
    }

    @Test
    void shouldClearTheSharedGameWhenTheLastPlayerLeaves() {
        final String playerId = service.addPlayer();
        service.removePlayer(playerId);

        final String nextPlayer = service.addPlayer();
        final GameSessionService.GameSnapshot state = service.snapshotFor(nextPlayer);

        assertFalse(state.notebookFound());
        assertFalse(state.chestSolved());
        assertFalse(state.gameWon());
        assertEquals(1, state.players().size());
    }

    private void moveToNotebook(final String playerId) {
        move(playerId, 0, -1, 19);
        move(playerId, -1, 0, 7);
    }

    private void moveToChest(final String playerId) {
        move(playerId, 0, -1, 19);
        move(playerId, 0, 1, 1);
        move(playerId, 1, 0, 50);
    }

    private void moveToChestWithoutClue(final String playerId) {
        move(playerId, 0, -1, 19);
        move(playerId, 0, 1, 1);
        move(playerId, 1, 0, 43);
    }

    private void moveToExit(final String playerId) {
        move(playerId, 1, 0, 0);
        move(playerId, -1, 0, 30);
        move(playerId, 0, -1, 19);
    }

    private void move(
            final String playerId,
            final int horizontal,
            final int vertical,
            final int steps) {
        for (int step = 0; step < steps; step++) {
            service.movePlayer(playerId, horizontal, vertical);
        }
    }

    private GameSessionService.PlayerSnapshot findPlayer(
            final GameSessionService.GameSnapshot snapshot,
            final String playerId) {
        return snapshot.players().stream()
                .filter(player -> player.id().equals(playerId))
                .findFirst()
                .orElseThrow();
    }
}
