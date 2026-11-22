package com.escapehub.gameplay.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.escapehub.gameplay.application.CreateGameSessionService;
import com.escapehub.gameplay.application.GameStateService;
import com.escapehub.gameplay.domain.GameSession;
import com.escapehub.gameplay.infrastructure.InMemoryGameRepository;
import com.escapehub.gameplay.infrastructure.InMemoryGameSessionRepository;
import com.escapehub.gameplay.infrastructure.InMemorySessionParticipantsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class GameStateControllerTest {

    @Test
    void mapsResultsToHttpStatuses() {
        final InMemoryGameSessionRepository sessions = new InMemoryGameSessionRepository();
        final GameStateController controller = new GameStateController(new GameStateService(
                sessions, new InMemorySessionParticipantsRepository(),
                new InMemoryGameRepository()));
        final GameSession session = new CreateGameSessionService(sessions).createSession("alice");

        assertEquals(HttpStatus.NOT_FOUND,
                controller.getGame(session.joinCode(), () -> "alice").getStatusCode());
        assertEquals(HttpStatus.OK,
                controller.enterGame(session.joinCode(), () -> "alice").getStatusCode());
        assertEquals(HttpStatus.OK,
                controller.getGame(session.joinCode(), () -> "alice").getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN,
                controller.enterGame(session.joinCode(), () -> "eve").getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND,
                controller.enterGame("ZZZZZZ", () -> "alice").getStatusCode());
    }
}
