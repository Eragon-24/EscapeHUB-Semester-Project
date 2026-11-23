package com.escapehub.gameplay.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.escapehub.gameplay.application.CreateGameSessionService;
import com.escapehub.gameplay.application.JoinGameSessionService;
import com.escapehub.gameplay.application.SessionParticipantsRepository;
import com.escapehub.gameplay.application.SessionUpdateChannel;
import com.escapehub.gameplay.domain.GameSession;
import com.escapehub.gameplay.domain.SessionParticipants;
import com.escapehub.gameplay.domain.SharedStateUpdate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class SharedStateControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private CreateGameSessionService createService;

    @Autowired
    private JoinGameSessionService joinService;

    @Autowired
    private SessionUpdateChannel channel;

    @Autowired
    private SessionParticipantsRepository participantsRepository;

    private MockMvc mockMvc;
    private GameSession session;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        session = createService.createSession("alice");
        joinService.joinSession(session.joinCode(), "bob");
    }

    private String statePath(final UUID sessionId) {
        return "/game-sessions/" + sessionId + "/state";
    }

    private String objectPath(final UUID sessionId, final String objectId) {
        return "/game-sessions/" + sessionId + "/objects/" + objectId + "/interactions";
    }

    @Test
    void participantChangesStateAndOtherParticipantsReceiveItInRealTime() throws Exception {
        final List<SharedStateUpdate> bobsStream = new ArrayList<>();
        channel.subscribe(session.id(), bobsStream::add);

        mockMvc.perform(post(statePath(session.id())).with(user("alice")).with(csrf())
                        .param("key", "door").param("value", "open"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.key").value("door"))
                .andExpect(jsonPath("$.author").value("alice"));

        assertEquals(1, bobsStream.size());
        assertEquals("open", bobsStream.get(0).value());
    }

    @Test
    void nonParticipantIsForbiddenAndNothingIsBroadcast() throws Exception {
        final List<SharedStateUpdate> stream = new ArrayList<>();
        channel.subscribe(session.id(), stream::add);

        mockMvc.perform(post(statePath(session.id())).with(user("mallory")).with(csrf())
                        .param("key", "door").param("value", "open"))
                .andExpect(status().isForbidden());

        assertTrue(stream.isEmpty());
    }

    @Test
    void invalidKeyIsRejectedWithABadRequestAndNotBroadcast() throws Exception {
        final List<SharedStateUpdate> stream = new ArrayList<>();
        channel.subscribe(session.id(), stream::add);

        mockMvc.perform(post(statePath(session.id())).with(user("alice")).with(csrf())
                        .param("key", "bad key!").param("value", "x"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        assertTrue(stream.isEmpty());
    }

    @Test
    void participantCanInteractWithAnObjectAndUpdateIsBroadcast() throws Exception {
        final List<SharedStateUpdate> updates = new ArrayList<>();
        channel.subscribe(session.id(), updates::add);

        mockMvc.perform(post(objectPath(session.id(), "lab-notebook")).with(user("alice")).with(csrf())
                        .param("action", "pickup"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value("inventory.lab-notebook"))
                .andExpect(jsonPath("$.value").value("true"));

        assertEquals(1, updates.size());
        assertEquals("true", updates.get(0).value());
    }

    @Test
    void invalidObjectInteractionIsRejectedAndNotBroadcast() throws Exception {
        final List<SharedStateUpdate> updates = new ArrayList<>();
        channel.subscribe(session.id(), updates::add);

        mockMvc.perform(post(objectPath(session.id(), "lab-notebook")).with(user("alice")).with(csrf())
                        .param("action", "open"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        assertTrue(updates.isEmpty());
    }

    @Test
    void nonParticipantCannotInteractWithObject() throws Exception {
        final List<SharedStateUpdate> updates = new ArrayList<>();
        channel.subscribe(session.id(), updates::add);

        mockMvc.perform(post(objectPath(session.id(), "lab-notebook")).with(user("mallory")).with(csrf())
                        .param("action", "open"))
                .andExpect(status().isForbidden());

        assertTrue(updates.isEmpty());
    }

    @Test
    void unknownSessionIsNotFound() throws Exception {
        mockMvc.perform(post(statePath(UUID.randomUUID())).with(user("alice")).with(csrf())
                        .param("key", "door").param("value", "open"))
                .andExpect(status().isNotFound());
    }

    @Test
    void changeWithoutCsrfTokenIsForbidden() throws Exception {
        mockMvc.perform(post(statePath(session.id())).with(user("alice"))
                        .param("key", "door").param("value", "open"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousVisitorIsSentToLogin() throws Exception {
        mockMvc.perform(post(statePath(session.id())).with(csrf())
                        .param("key", "door").param("value", "open"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/game-sessions/" + session.id() + "/events"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void participantCanOpenTheEventStream() throws Exception {
        mockMvc.perform(get("/game-sessions/" + session.id() + "/events").with(user("bob")))
                .andExpect(request().asyncStarted());
    }

    @Test
    void nonParticipantCannotOpenTheEventStream() throws Exception {
        mockMvc.perform(get("/game-sessions/" + session.id() + "/events").with(user("mallory")))
                .andExpect(status().isForbidden());
    }

    @Test
    void eventStreamOfAnUnknownSessionIsNotFound() throws Exception {
        mockMvc.perform(get("/game-sessions/" + UUID.randomUUID() + "/events").with(user("alice")))
                .andExpect(status().isNotFound());
    }
    @Test
    void inactiveSessionCannotOpenTheEventStream() throws Exception {
        final SessionParticipants participants = participantsRepository.findBySessionId(session.id()).orElseThrow();
        participants.deactivate();
        participantsRepository.save(participants);

        mockMvc.perform(get("/game-sessions/" + session.id() + "/events").with(user("alice")))
                .andExpect(status().isConflict());
    }

}
