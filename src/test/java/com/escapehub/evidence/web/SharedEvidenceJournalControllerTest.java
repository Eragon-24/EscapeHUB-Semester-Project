package com.escapehub.evidence.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.escapehub.evidence.application.SharedEvidenceJournalService;
import com.escapehub.evidence.infrastructure.InMemoryEvidenceRepository;
import com.escapehub.gameplay.application.CreateGameSessionService;
import com.escapehub.gameplay.application.JoinGameSessionService;
import com.escapehub.gameplay.domain.GameSession;
import com.escapehub.gameplay.infrastructure.InMemoryGameSessionRepository;
import com.escapehub.gameplay.infrastructure.InMemorySessionParticipantsRepository;
import java.security.Principal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SharedEvidenceJournalControllerTest {

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
        final SharedEvidenceJournalService journalService = new SharedEvidenceJournalService(
                new InMemoryEvidenceRepository(),
                sessionRepository,
                participantsRepository
        );
        session = createService.createSession("player1");
        joinService.joinSession(session.joinCode(), "player2");

        mockMvc = MockMvcBuilders.standaloneSetup(
                new SharedEvidenceJournalController(journalService)
        ).build();
    }

    @Test
    void shouldAddEvidenceAndExposeItToAllSessionParticipants() throws Exception {
        final Principal player1 = () -> "player1";
        final Principal player2 = () -> "player2";

        mockMvc.perform(post("/api/v1/game-sessions/{id}/journal", session.id())
                        .principal(player1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evidenceId\": \"LAB_NOTEBOOK\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value(session.id().toString()))
                .andExpect(jsonPath("$.title").value("Σημειωματάριο Εργαστηρίου"));

        mockMvc.perform(get("/api/v1/game-sessions/{id}/journal", session.id())
                        .principal(player2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sessionId").value(session.id().toString()))
                .andExpect(jsonPath("$[0].title").value("Σημειωματάριο Εργαστηρίου"));
    }

    @Test
    void shouldRejectDuplicateAndIneligibleEvidence() throws Exception {
        final Principal player1 = () -> "player1";

        mockMvc.perform(post("/api/v1/game-sessions/{id}/journal", session.id())
                        .principal(player1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evidenceId\": \"CHEST_INSCRIPTION\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/game-sessions/{id}/journal", session.id())
                        .principal(player1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evidenceId\": \"CHEST_INSCRIPTION\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/game-sessions/{id}/journal", session.id())
                        .principal(player1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evidenceId\": \"INVALID_CLUE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldEnforceAuthenticationAndSessionMembership() throws Exception {
        final Principal intruder = () -> "intruder";
        final Principal player1 = () -> "player1";

        mockMvc.perform(post("/api/v1/game-sessions/{id}/journal", session.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evidenceId\": \"LAB_NOTEBOOK\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/game-sessions/{id}/journal", session.id())
                        .principal(intruder)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evidenceId\": \"LAB_NOTEBOOK\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/game-sessions/{id}/journal", UUID.randomUUID())
                        .principal(player1))
                .andExpect(status().isNotFound());
    }
}