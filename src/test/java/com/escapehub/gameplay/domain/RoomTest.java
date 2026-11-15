package com.escapehub.gameplay.domain;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomTest {

    @Test
    void shouldCreateDefaultDemoRoomWithNameDescriptionAndSelectableObjects() {
        final Room room = new Room();

        assertEquals("Εγκαταλελειμμένο Εργαστήριο", room.getName());
        assertFalse(room.getDescription().isBlank());
        assertFalse(room.getSelectableObjects().isEmpty());
        assertTrue(room.getSelectableObjects().contains("Κλειδωμένο Σεντούκι"));
    }

    @Test
    void shouldRejectBlankRoomName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Room(" ", "Περιγραφή", List.of("Σεντούκι"))
        );
    }
}