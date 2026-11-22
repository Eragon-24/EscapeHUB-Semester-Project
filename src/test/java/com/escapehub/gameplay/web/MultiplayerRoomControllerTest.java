package com.escapehub.gameplay.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

class MultiplayerRoomControllerTest {

    @Test
    void shouldDisplayRoomAndAllowAuthenticatedCreatorToStartSession() {
        final MultiplayerRoomController controller = new MultiplayerRoomController();
        final ExtendedModelMap model = new ExtendedModelMap();

        final String view = controller.showRoom(() -> "playerOne", model);

        assertEquals("multiplayer-room", view);
        assertEquals("Εγκαταλελειμμένο Εργαστήριο", model.get("roomName"));
        assertTrue(model.containsAttribute("roomDescription"));
        assertEquals(Boolean.TRUE, model.get("canCreateSession"));
    }

    @Test
    void shouldDisplayRoomWithoutSessionCreationForAnonymousVisitor() {
        final MultiplayerRoomController controller = new MultiplayerRoomController();
        final ExtendedModelMap model = new ExtendedModelMap();

        final String view = controller.showRoom(() -> "anonymousUser", model);

        assertEquals("multiplayer-room", view);
        assertFalse((Boolean) model.get("canCreateSession"));
    }
}
