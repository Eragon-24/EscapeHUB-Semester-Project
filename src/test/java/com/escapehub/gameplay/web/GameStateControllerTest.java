package com.escapehub.gameplay.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.escapehub.gameplay.application.CreateGameSessionService;
import com.escapehub.gameplay.application.JoinGameSessionService;
import com.escapehub.gameplay.domain.Game;
import com.escapehub.gameplay.domain.GameSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;


@SpringBootTest
class GameStateControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private CreateGameSessionService createService;

    @Autowired
    private JoinGameSessionService joinService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void anonymousRequestIsNotServed() throws Exception {
        mockMvc.perform(get("/game-sessions/ABC234/game"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        mockMvc.perform(post("/game-sessions/ABC234/game").with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void firstEntryInitializesAndLaterEntryLoadsSameState() throws Exception {
        final GameSession session = createService.createSession("http_alice");
        joinService.joinSession(session.joinCode(), "http_bob");
        final String url = "/game-sessions/" + session.joinCode() + "/game";

        mockMvc.perform(get(url).with(user("http_alice")))
                .andExpect(status().isNotFound());
        mockMvc.perform(post(url).with(user("http_alice")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentRoomId").value(Game.INITIAL_ROOM_ID))
                .andExpect(jsonPath("$.sessionId").value(session.id().toString()));
        mockMvc.perform(post(url).with(user("http_bob")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(session.id().toString()));
        mockMvc.perform(get(url).with(user("http_bob")))
                .andExpect(status().isOk());
    }

    @Test
    void mapsErrorsToHttpStatuses() throws Exception {
        final GameSession session = createService.createSession("http_carol");

        mockMvc.perform(post("/game-sessions/" + session.joinCode() + "/game")
                        .with(user("http_mallory")).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/game-sessions/ZZZZZZ/game")
                        .with(user("http_carol")).with(csrf()))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/game-sessions/%20/game")
                        .with(user("http_carol")).with(csrf()))
                .andExpect(status().isBadRequest());
    }
}