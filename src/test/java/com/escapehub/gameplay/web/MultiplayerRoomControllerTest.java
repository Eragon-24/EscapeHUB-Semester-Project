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

        final String view = controller.showRoom(null, null, () -> "playerOne", model);

        assertEquals("multiplayer-room", view);
        assertEquals("Εγκαταλελειμμένο Εργαστήριο", model.get("roomName"));
        assertTrue(model.containsAttribute("roomDescription"));
        assertEquals(Boolean.TRUE, model.get("canCreateSession"));
    }

    @Test
    void shouldDisplayRoomWithoutSessionCreationForAnonymousVisitor() {
        final MultiplayerRoomController controller = new MultiplayerRoomController();
        final ExtendedModelMap model = new ExtendedModelMap();

        final String view = controller.showRoom(null, null, () -> "anonymousUser", model);

        assertEquals("multiplayer-room", view);
        assertFalse((Boolean) model.get("canCreateSession"));
    }

    @Test
    void shouldExposeActiveSessionCodeSoStateReloadsAfterRefresh() {
        final MultiplayerRoomController controller = new MultiplayerRoomController();
        final ExtendedModelMap model = new ExtendedModelMap();

        controller.showRoom("abc234", null, () -> "playerOne", model);

        assertEquals("ABC234", model.get("activeSessionCode"));
    }

    @Test
    void shouldExposeActiveSessionIdWithoutReadingRequestParametersInTemplate() {
        final MultiplayerRoomController controller = new MultiplayerRoomController();
        final ExtendedModelMap model = new ExtendedModelMap();

        controller.showRoom("ABC234", "session-123", () -> "playerOne", model);

        assertEquals("session-123", model.get("activeSessionId"));
    }

    @Test
    void shouldIgnoreMalformedSessionCodeAndAnonymousVisitors() {
        final MultiplayerRoomController controller = new MultiplayerRoomController();
        final ExtendedModelMap malformed = new ExtendedModelMap();
        final ExtendedModelMap anonymous = new ExtendedModelMap();

        controller.showRoom("<script>", null, () -> "playerOne", malformed);
        controller.showRoom("ABC234", null, () -> "anonymousUser", anonymous);

        assertFalse(malformed.containsAttribute("activeSessionCode"));
        assertFalse(anonymous.containsAttribute("activeSessionCode"));
    }
}
