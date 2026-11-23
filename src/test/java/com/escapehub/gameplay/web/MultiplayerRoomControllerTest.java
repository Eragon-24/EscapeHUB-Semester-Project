package com.escapehub.gameplay.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.escapehub.gameplay.domain.GameSession;
import com.escapehub.gameplay.domain.Room;
import com.escapehub.gameplay.domain.SessionParticipants;
import com.escapehub.gameplay.application.RoomItemCatalog;
import com.escapehub.gameplay.infrastructure.InMemoryGameSessionRepository;
import com.escapehub.gameplay.infrastructure.InMemorySessionParticipantsRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.ExtendedModelMap;

class MultiplayerRoomControllerTest {

    private InMemoryGameSessionRepository gameSessionRepository;
    private InMemorySessionParticipantsRepository participantsRepository;
    private MultiplayerRoomController controller;

    @BeforeEach
    void setUp() {
        gameSessionRepository = new InMemoryGameSessionRepository();
        participantsRepository = new InMemorySessionParticipantsRepository();
        controller = new MultiplayerRoomController(
                gameSessionRepository, participantsRepository, new RoomItemCatalog(java.util.List.of("lab-notebook")));
    }

    @Test
    void shouldDisplayRoomAndAllowAuthenticatedCreatorToStartSession() {
        final ExtendedModelMap model = new ExtendedModelMap();

        final String view = controller.showRoom(null, null, () -> "playerOne", model);

        assertEquals("multiplayer-room", view);
        assertEquals("Εγκαταλελειμμένο Εργαστήριο", model.get("roomName"));
        assertTrue(model.containsAttribute("roomDescription"));
        assertEquals(
                List.of(new MultiplayerRoomController.InspectableObject(
                        Room.NOTEBOOK, Room.NOTEBOOK)),
                model.get("inspectableObjects")
        );
        assertEquals(Boolean.TRUE, model.get("canCreateSession"));
    }

    @Test
    void shouldDisplayRoomWithoutSessionCreationForAnonymousVisitor() {
        final ExtendedModelMap model = new ExtendedModelMap();

        final String view = controller.showRoom(null, null, () -> "anonymousUser", model);

        assertEquals("multiplayer-room", view);
        assertFalse((Boolean) model.get("canCreateSession"));
    }

    @Test
    void shouldExposeActiveSessionCodeSoStateReloadsAfterRefresh() {
        final ExtendedModelMap model = new ExtendedModelMap();

        controller.showRoom("abc234", null, () -> "playerOne", model);

        assertEquals("ABC234", model.get("activeSessionCode"));
    }

    @Test
    void shouldExposeActiveSessionIdWithoutReadingRequestParametersInTemplate() {
        final ExtendedModelMap model = new ExtendedModelMap();

        controller.showRoom("ABC234", "session-123", () -> "playerOne", model);

        assertEquals("session-123", model.get("activeSessionId"));
    }

    @Test
    void shouldIgnoreMalformedSessionCodeAndAnonymousVisitors() {
        final ExtendedModelMap malformed = new ExtendedModelMap();
        final ExtendedModelMap anonymous = new ExtendedModelMap();

        controller.showRoom("<script>", null, () -> "playerOne", malformed);
        controller.showRoom("ABC234", null, () -> "anonymousUser", anonymous);

        assertFalse(malformed.containsAttribute("activeSessionCode"));
        assertFalse(anonymous.containsAttribute("activeSessionCode"));
    }

    @Test
    void shouldNotAllowAuthenticatedNonParticipantToLeaveSessionFromUi() {
        final GameSession session = createSession("creator1");
        final SessionParticipants participants = new SessionParticipants(session.id(), "creator1");
        participants.addPlayer("member1");
        participantsRepository.save(participants);
        final ExtendedModelMap model = new ExtendedModelMap();

        controller.showRoom(session.joinCode(), session.id().toString(), () -> "intruder", model);

        assertEquals(Boolean.FALSE, model.get("canLeaveSession"));
    }

    @Test
    void shouldAllowActiveParticipantToLeaveSessionFromUi() {
        final GameSession session = createSession("creator1");
        final SessionParticipants participants = new SessionParticipants(session.id(), "creator1");
        participants.addPlayer("member1");
        participantsRepository.save(participants);
        final ExtendedModelMap model = new ExtendedModelMap();

        controller.showRoom(session.joinCode(), session.id().toString(), () -> "member1", model);

        assertEquals(Boolean.TRUE, model.get("canLeaveSession"));
    }

    @Test
    void shouldAllowCreatorToLeaveBeforeParticipantStateIsCreated() {
        final GameSession session = createSession("creator1");
        final ExtendedModelMap model = new ExtendedModelMap();

        controller.showRoom(session.joinCode(), session.id().toString(), () -> "creator1", model);

        assertEquals(Boolean.TRUE, model.get("canLeaveSession"));
    }

    @Test
    void shouldServeExistingRoomInspectionInformationForInspectableObject() throws Exception {
        final MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();

        mockMvc.perform(get("/api/room/objects/{objectName}/inspection", Room.NOTEBOOK))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(Room.NOTEBOOK))
                .andExpect(jsonPath("$.description").value(Room.NOTEBOOK_CLUE));
    }

    @Test
    void shouldRejectUnknownOrUnavailableObjectInspection() throws Exception {
        final MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();

        mockMvc.perform(get("/api/room/objects/unknown/inspection"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(
                "/api/room/objects/{objectName}/inspection",
                "Κλειδωμένο Σεντούκι"
        )).andExpect(status().isNotFound());
    }

    private GameSession createSession(final String creatorUsername) {
        return gameSessionRepository.save(new GameSession(
                UUID.randomUUID(), "ABC234", creatorUsername, Instant.now()));
    }
}
