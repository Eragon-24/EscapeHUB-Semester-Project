package com.escapehub.gameplay.domain;

import java.util.List;
import java.util.Optional;
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
    void shouldRevealClueWhenNotebookIsSelected() {
        final Room room = new Room();

        assertEquals(Optional.of(Room.NOTEBOOK_CLUE), room.inspect(Room.NOTEBOOK));
        assertTrue(Room.NOTEBOOK_CLUE.matches(".*\\d-\\d-\\d-\\d-\\d-\\d.*"));
    }

    @Test
    void shouldNotRevealClueForObjectWithoutClue() {
        assertTrue(new Room().inspect("Πόρτα Εξόδου").isEmpty());
    }

    @Test
    void shouldRejectBlankRoomName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Room(" ", "Περιγραφή", List.of("Σεντούκι"))
        );
    }
}