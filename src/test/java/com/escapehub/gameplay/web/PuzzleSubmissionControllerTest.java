package com.escapehub.gameplay.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.escapehub.gameplay.application.CreateGameSessionService;
import com.escapehub.gameplay.application.JoinGameSessionService;
import com.escapehub.gameplay.application.SessionUpdateChannel;
import com.escapehub.gameplay.application.SharedStateService;
import com.escapehub.gameplay.domain.GameSession;
import com.escapehub.gameplay.domain.GameState;
import com.escapehub.gameplay.domain.SharedStateUpdate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@SpringBootTest
class PuzzleSubmissionControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private CreateGameSessionService createService;

    @Autowired
    private JoinGameSessionService joinService;

    @Autowired
    private SessionUpdateChannel channel;

    @Autowired
    private SharedStateService sharedState;

    private MockMvc mockMvc;
    private GameSession session;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        session = createService.createSession("alice");
        joinService.joinSession(session.joinCode(), "bob");
    }

    private void assertUntouched() {
        final GameState state = sharedState.snapshot(session.id(), "alice");
        assertTrue(state.completedPuzzleIds().isEmpty());
        assertTrue(state.collectedItems().isEmpty());
        assertFalse(state.exitUnlocked());
    }

    @Test
    void exactlyOneHandlerAcceptsPuzzleAnswers() {
        final long handlers = context
                .getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class)
                .getHandlerMethods().keySet().stream()
                .filter(mapping -> mapping.getPatternValues()
                        .contains("/game-sessions/{sessionId}/puzzles/{puzzleId}/answer"))
                .count();

        assertEquals(1, handlers);
    }

    private String answerPath(final UUID sessionId, final String puzzleId) {
        return "/game-sessions/" + sessionId + "/puzzles/" + puzzleId + "/answer";
    }

    @Test
    void correctAnswerIsAcceptedAndReachesTheOtherParticipantsInRealTime() throws Exception {
        final List<SharedStateUpdate> bobsStream = new ArrayList<>();
        channel.subscribe(session.id(), bobsStream::add);

        mockMvc.perform(post(answerPath(session.id(), "chest")).with(user("alice")).with(csrf())
                        .param("answer", "483920"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.puzzleId").value("chest"))
                .andExpect(jsonPath("$.message").exists());

        assertEquals(List.of("progress.puzzles.chest", "progress.items.LAB_KEY", "game.exitUnlocked"),
                bobsStream.stream().map(SharedStateUpdate::key).toList());
        final GameState seenByBob = sharedState.snapshot(session.id(), "bob");
        assertTrue(seenByBob.completedPuzzleIds().contains("chest"));
        assertTrue(seenByBob.exitUnlocked());
    }

    @Test
    void wrongAnswerIsRejectedWithFeedbackAndNothingIsBroadcast() throws Exception {
        final List<SharedStateUpdate> stream = new ArrayList<>();
        channel.subscribe(session.id(), stream::add);

        mockMvc.perform(post(answerPath(session.id(), "chest")).with(user("alice")).with(csrf())
                        .param("answer", "000000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.message").exists());

        assertTrue(stream.isEmpty());
        assertUntouched();
    }

    @Test
    void solvingTwiceReportsAlreadySolvedAndBroadcastsOnlyOnce() throws Exception {
        final List<SharedStateUpdate> stream = new ArrayList<>();
        channel.subscribe(session.id(), stream::add);
        mockMvc.perform(post(answerPath(session.id(), "chest")).with(user("alice")).with(csrf())
                .param("answer", "483920")).andExpect(status().isOk());

        mockMvc.perform(post(answerPath(session.id(), "chest")).with(user("bob")).with(csrf())
                        .param("answer", "483920"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ALREADY_SOLVED"));

        assertEquals(3, stream.size());
    }

    @Test
    void clientCannotMarkAPuzzleSolvedThroughTheGenericStateEndpoint() throws Exception {
        final List<SharedStateUpdate> stream = new ArrayList<>();
        channel.subscribe(session.id(), stream::add);

        mockMvc.perform(post("/game-sessions/" + session.id() + "/state").with(user("alice")).with(csrf())
                        .param("key", "progress.puzzles.chest").param("value", "true"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/game-sessions/" + session.id() + "/state").with(user("alice")).with(csrf())
                        .param("key", "game.exitUnlocked").param("value", "true"))
                .andExpect(status().isBadRequest());

        assertTrue(stream.isEmpty());
        assertUntouched();
        mockMvc.perform(post(answerPath(session.id(), "chest")).with(user("alice")).with(csrf())
                        .param("answer", "000000"))
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void nonParticipantIsForbidden() throws Exception {
        mockMvc.perform(post(answerPath(session.id(), "chest")).with(user("mallory")).with(csrf())
                        .param("answer", "483920"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/game-sessions/" + session.id() + "/puzzles").with(user("mallory")))
                .andExpect(status().isForbidden());
        assertUntouched();
    }

    @Test
    void unknownPuzzleAndUnknownSessionAreNotFound() throws Exception {
        mockMvc.perform(post(answerPath(session.id(), "vault")).with(user("alice")).with(csrf())
                        .param("answer", "1"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post(answerPath(UUID.randomUUID(), "chest")).with(user("alice")).with(csrf())
                        .param("answer", "1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void submissionWithoutCsrfTokenOrLoginIsNotAccepted() throws Exception {
        mockMvc.perform(post(answerPath(session.id(), "chest")).with(user("alice")).param("answer", "483920"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(answerPath(session.id(), "chest")).with(csrf()).param("answer", "483920"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void puzzleListShowsPromptButNeverTheSolution() throws Exception {
        final MvcResult result = mockMvc.perform(get("/game-sessions/" + session.id() + "/puzzles")
                        .with(user("bob")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("chest"))
                .andExpect(jsonPath("$[0].prompt").exists())
                .andReturn();

        final String body = result.getResponse().getContentAsString();
        assertFalse(body.contains("483920"));
        assertFalse(body.contains("solution"));
    }
}
