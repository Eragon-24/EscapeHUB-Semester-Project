package com.escapehub.puzzle.application;

import com.escapehub.puzzle.domain.PuzzleDefinition;
import java.util.List;
import java.util.Optional;

/**
 * US-022: source of the configured puzzles (prompt and solution).
 */
public interface PuzzleCatalog {

    /**
     * Returns all puzzles that can be attempted.
     *
     * @return the puzzle definitions
     */
    List<PuzzleDefinition> findAll();

    /**
     * Finds a puzzle by id.
     *
     * @param id the puzzle id
     * @return the definition, if such a puzzle exists
     */
    Optional<PuzzleDefinition> findById(String id);
}
