package com.escapehub.gameplay.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.escapehub.gameplay.domain.Room;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.ExtendedModelMap;

class MultiplayerRoomControllerTest {

    @Test
    void shouldDisplayRoomAndAllowAuthenticatedCreatorToStartSession() {
        final MultiplayerRoomController controller = new MultiplayerRoomController();
        final ExtendedModelMap model = new ExtendedModelMap();

        final String view = controller.showRoom(null, () -> "playerOne", model);

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
        final MultiplayerRoomController controller = new MultiplayerRoomController();
        final ExtendedModelMap model = new ExtendedModelMap();

        final String view = controller.showRoom(null, () -> "anonymousUser", model);

        assertEquals("multiplayer-room", view);
        assertFalse((Boolean) model.get("canCreateSession"));
    }

    @Test
    void shouldServeExistingRoomInspectionInformationForInspectableObject() throws Exception {
        final MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new MultiplayerRoomController())
                .build();

        mockMvc.perform(get("/api/room/objects/{objectName}/inspection", Room.NOTEBOOK))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(Room.NOTEBOOK))
                .andExpect(jsonPath("$.description").value(Room.NOTEBOOK_CLUE));
    }

    @Test
    void shouldRejectUnknownOrUnavailableObjectInspection() throws Exception {
        final MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new MultiplayerRoomController())
                .build();

        mockMvc.perform(get("/api/room/objects/unknown/inspection"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(
                "/api/room/objects/{objectName}/inspection",
                "Κλειδωμένο Σεντούκι"
        )).andExpect(status().isNotFound());
    }
}
