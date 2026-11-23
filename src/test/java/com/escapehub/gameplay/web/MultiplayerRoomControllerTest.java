package com.escapehub.gameplay.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.escapehub.gameplay.infrastructure.InMemoryGameSessionRepository;
import com.escapehub.gameplay.infrastructure.InMemorySessionParticipantsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

class MultiplayerRoomControllerTest {

    @Test
    void shouldDisplayRoomAndAllowAuthenticatedCreatorToStartSession() {
        final MultiplayerRoomController controller = createController();
        final ExtendedModelMap model = new ExtendedModelMap();

        final String view = controller.showRoom(null, () -> "playerOne", model);

        assertEquals("multiplayer-room", view);
        assertEquals("Εγκαταλελειμμένο Εργαστήριο", model.get("roomName"));
        assertTrue(model.containsAttribute("roomDescription"));
        assertEquals(Boolean.TRUE, model.get("canCreateSession"));
    }

    @Test
    void shouldDisplayRoomWithoutSessionCreationForAnonymousVisitor() {
        final MultiplayerRoomController controller = createController();
        final ExtendedModelMap model = new ExtendedModelMap();

        final String view = controller.showRoom(null, () -> "anonymousUser", model);

        assertEquals("multiplayer-room", view);
        assertFalse((Boolean) model.get("canCreateSession"));
    }

    @Test
    void shouldExposeActiveSessionCodeSoStateReloadsAfterRefresh() {
        final MultiplayerRoomController controller = createController();
        final ExtendedModelMap model = new ExtendedModelMap();

        controller.showRoom("abc234", () -> "playerOne", model);

        assertEquals("ABC234", model.get("activeSessionCode"));
    }

    @Test
    void shouldIgnoreMalformedSessionCodeAndAnonymousVisitors() {
        final MultiplayerRoomController controller = createController();
        final ExtendedModelMap malformed = new ExtendedModelMap();
        final ExtendedModelMap anonymous = new ExtendedModelMap();

        controller.showRoom("<script>", () -> "playerOne", malformed);
        controller.showRoom("ABC234", () -> "anonymousUser", anonymous);

        assertFalse(malformed.containsAttribute("activeSessionCode"));
        assertFalse(anonymous.containsAttribute("activeSessionCode"));
    }

    private MultiplayerRoomController createController() {
        return new MultiplayerRoomController(
                new InMemoryGameSessionRepository(),
                new InMemorySessionParticipantsRepository());
    }
}
