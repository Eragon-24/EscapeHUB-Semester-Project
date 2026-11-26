package com.escapehub.evidence.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.escapehub.gameplay.application.CreateGameSessionService;
import com.escapehub.gameplay.application.JoinGameSessionService;
import com.escapehub.gameplay.domain.GameSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class JournalSecurityIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private CreateGameSessionService createSessionService;

    @Autowired
    private JoinGameSessionService joinSessionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void enforcesSpringSecurityAndSharesAddedEvidenceAcrossParticipants() throws Exception {
        final GameSession session = createSessionService.createSession("alice");
        joinSessionService.joinSession(session.joinCode(), "bob");

        // Unauthenticated POST is rejected by Spring Security
        mockMvc.perform(post("/game-sessions/{joinCode}/journal", session.joinCode())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evidenceId\": \"LAB_NOTEBOOK\"}"))
                .andExpect(status().is3xxRedirection());

        // Outsider receives 403 Forbidden
        mockMvc.perform(post("/game-sessions/{joinCode}/journal", session.joinCode())
                        .with(user("eve"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evidenceId\": \"LAB_NOTEBOOK\"}"))
                .andExpect(status().isForbidden());

        // Active participant adds eligible evidence -> 201 Created
        mockMvc.perform(post("/game-sessions/{joinCode}/journal", session.joinCode())
                        .with(user("bob"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evidenceId\": \"LAB_NOTEBOOK\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.evidenceId").value("LAB_NOTEBOOK"))
                .andExpect(jsonPath("$.title").value("Σημειωματάριο Εργαστηρίου"));

        // Duplicate evidenceId in same session -> 409 Conflict
        mockMvc.perform(post("/game-sessions/{joinCode}/journal", session.joinCode())
                        .with(user("alice"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evidenceId\": \"LAB_NOTEBOOK\"}"))
                .andExpect(status().isConflict());

        // Creator sees the shared entry via GET -> 200 OK
        mockMvc.perform(get("/game-sessions/{joinCode}/journal", session.joinCode())
                        .with(user("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].evidenceId").value("LAB_NOTEBOOK"));
    }
}