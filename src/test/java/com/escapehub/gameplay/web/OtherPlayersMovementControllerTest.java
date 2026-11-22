package com.escapehub.gameplay.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.escapehub.gameplay.application.CreateGameSessionService;
import com.escapehub.gameplay.application.JoinGameSessionService;
import com.escapehub.gameplay.application.OtherPlayersMovementService;
import com.escapehub.gameplay.domain.GameSession;
import com.escapehub.gameplay.infrastructure.InMemoryGameSessionRepository;
import com.escapehub.gameplay.infrastructure.InMemorySessionParticipantsRepository;
import com.escapehub.gameplay.infrastructure.InMemorySessionPlayerPositionsRepository;
import java.security.Principal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class OtherPlayersMovementControllerTest {

    private MockMvc mockMvc;
    private GameSession session;

    @BeforeEach
    void setUp() {
        final InMemoryGameSessionRepository sessionRepository = new InMemoryGameSessionRepository();
        final InMemorySessionParticipantsRepository participantsRepository =
                new InMemorySessionParticipantsRepository();
        final CreateGameSessionService createService =
                new CreateGameSessionService(sessionRepository);
        final JoinGameSessionService joinService =
                new JoinGameSessionService(sessionRepository, participantsRepository);
        final OtherPlayersMovementService movementService = new OtherPlayersMovementService(
                new InMemorySessionPlayerPositionsRepository(),
                sessionRepository,
                participantsRepository
        );
        session = createService.createSession("player1");
        joinService.joinSession(session.joinCode(), "player2");

        mockMvc = MockMvcBuilders.standaloneSetup(
                new OtherPlayersMovementController(movementService)
        ).build();
    }

    @Test
    void shouldUpdateAndFetchOtherPlayersPositionsViaHttp() throws Exception {
        final Principal player1 = () -> "player1";
        final Principal player2 = () -> "player2";

        mockMvc.perform(put("/api/v1/game-sessions/{id}/player-position", session.id())
                        .principal(player2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"x\": 520.0, \"y\": 360.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.x").value(520.0))
                .andExpect(jsonPath("$.y").value(360.0));

        mockMvc.perform(get("/api/v1/game-sessions/{id}/other-players", session.id())
                        .principal(player1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("player2"))
                .andExpect(jsonPath("$[0].position.x").value(520.0))
                .andExpect(jsonPath("$[0].position.y").value(360.0));

        mockMvc.perform(delete("/api/v1/game-sessions/{id}/player-position", session.id())
                        .principal(player2))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/game-sessions/{id}/other-players", session.id())
                        .principal(player1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldRejectOutOfBoundsOrObstacleCoordinatesWithBadRequest() throws Exception {
        final Principal player1 = () -> "player1";

        mockMvc.perform(put("/api/v1/game-sessions/{id}/player-position", session.id())
                        .principal(player1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"x\": 10.0, \"y\": 10.0}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/v1/game-sessions/{id}/player-position", session.id())
                        .principal(player1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"x\": 250.0, \"y\": 230.0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnUnauthorizedForUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/api/v1/game-sessions/{id}/other-players", session.id()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/v1/game-sessions/{id}/player-position", session.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"x\": 480, \"y\": 490}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnForbiddenForNonParticipantAndNotFoundForUnknownSession() throws Exception {
        final Principal intruder = () -> "intruder";
        final Principal player1 = () -> "player1";

        mockMvc.perform(get("/api/v1/game-sessions/{id}/other-players", session.id())
                        .principal(intruder))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/game-sessions/{id}/other-players", UUID.randomUUID())
                        .principal(player1))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnBadRequestWhenCoordinatesAreMissing() throws Exception {
        final Principal player1 = () -> "player1";

        mockMvc.perform(put("/api/v1/game-sessions/{id}/player-position", session.id())
                        .principal(player1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}