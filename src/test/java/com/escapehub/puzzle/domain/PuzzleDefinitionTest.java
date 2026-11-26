package com.escapehub.puzzle.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.escapehub.puzzle.infrastructure.InMemoryPuzzleCatalog;
import org.junit.jupiter.api.Test;

class PuzzleDefinitionTest {

    @Test
    void validDefinitionKeepsItsFields() {
        final PuzzleDefinition puzzle = new PuzzleDefinition("chest", "Ερώτηση;", "42");

        assertEquals("chest", puzzle.id());
        assertEquals("Ερώτηση;", puzzle.prompt());
        assertEquals("42", puzzle.solution());
    }

    @Test
    void invalidDefinitionsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new PuzzleDefinition("Bad Id", "p", "s"));
        assertThrows(IllegalArgumentException.class, () -> new PuzzleDefinition(null, "p", "s"));
        assertThrows(IllegalArgumentException.class, () -> new PuzzleDefinition("ok", " ", "s"));
        assertThrows(IllegalArgumentException.class, () -> new PuzzleDefinition("ok", "p", ""));
    }

    @Test
    void catalogProvidesTheChestPuzzle() {
        final InMemoryPuzzleCatalog catalog = new InMemoryPuzzleCatalog();

        assertEquals(1, catalog.findAll().size());
        assertTrue(catalog.findById("chest").isPresent());
        assertTrue(catalog.findById("nope").isEmpty());
    }
}
