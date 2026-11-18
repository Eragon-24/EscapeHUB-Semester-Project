package com.escapehub.gameplay.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GameControllerTest {

    @Test
    void shouldDisplayBrowserMultiplayerGame() {
        final GameController controller = new GameController();

        assertEquals("game", controller.showGame());
    }
}
