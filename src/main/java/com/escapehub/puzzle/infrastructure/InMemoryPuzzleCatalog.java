package com.escapehub.puzzle.infrastructure;

import com.escapehub.puzzle.application.PuzzleCatalog;
import com.escapehub.puzzle.domain.PuzzleDefinition;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * US-022: the puzzle content of the demo, kept in code. It uses the existing chest puzzle of the laboratory
 * (six-digit code found in the notebook); new puzzle content is outside this story.
 */
@Component
public class InMemoryPuzzleCatalog implements PuzzleCatalog {

    private static final List<PuzzleDefinition> PUZZLES = List.of(new PuzzleDefinition(
            "chest",
            "Το σεντούκι έχει κλειδαριά με εξάψηφο κωδικό. Πληκτρολόγησε τον κωδικό.",
            "483920"));

    @Override
    public List<PuzzleDefinition> findAll() {
        return PUZZLES;
    }

    @Override
    public Optional<PuzzleDefinition> findById(final String id) {
        return PUZZLES.stream().filter(p -> p.id().equals(id)).findFirst();
    }
}
