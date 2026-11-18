package com.escapehub.gameplay.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        final double secondPlayerInitialX = service.snapshotFor(secondPlayer)
                .players().get(1).x();

        service.movePlayer(firstPlayer, 1, 0);

        final GameSessionService.GameSnapshot state = service.snapshotFor(secondPlayer);
        final GameSessionService.PlayerSnapshot movedPlayer = state.players().get(0);
        final GameSessionService.PlayerSnapshot otherPlayer = state.players().get(1);

        assertEquals(firstPlayer, movedPlayer.id());
        assertTrue(movedPlayer.x() > 360);
        assertEquals(secondPlayerInitialX, otherPlayer.x());
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
}
