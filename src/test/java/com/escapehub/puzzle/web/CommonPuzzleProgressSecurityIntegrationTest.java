package com.escapehub.puzzle.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.escapehub.gameplay.application.CreateGameSessionService;
import com.escapehub.gameplay.domain.GameSession;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** Exercises the puzzle-progress endpoint through the real Spring Security filter chain. */
@SpringBootTest
class CommonPuzzleProgressSecurityIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private CreateGameSessionService createGameSessionService;

    private MockMvc mockMvc;
    private GameSession session;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        session = createGameSessionService.createSession("alice");
    }

    private String progressPath(final UUID sessionId) {
        return "/api/v1/game-sessions/" + sessionId + "/puzzle-progress";
    }

    @Test
    void authenticatedSessionCreatorCanReadProgress() throws Exception {
        mockMvc.perform(get(progressPath(session.id())).with(user("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPuzzles").value(1))
                .andExpect(jsonPath("$.solvedCount").value(0));
    }

    @Test
    void anonymousRequestIsRedirectedToLoginBySpringSecurity() throws Exception {
        mockMvc.perform(get(progressPath(session.id())))
                .andExpect(status().is3xxRedirection())
                .andExpect(status().redirectedUrl("/login"));
    }

    @Test
    void authenticatedNonParticipantReceivesForbidden() throws Exception {
        mockMvc.perform(get(progressPath(session.id())).with(user("mallory")))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedRequestForUnknownSessionReceivesNotFound() throws Exception {
        mockMvc.perform(get(progressPath(UUID.randomUUID())).with(user("alice")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Η συνεδρία δεν βρέθηκε."));
    }
}
