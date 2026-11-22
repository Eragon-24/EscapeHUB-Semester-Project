package com.escapehub.puzzle.domain;

import java.util.regex.Pattern;

/**
 * US-022: the configured definition of a puzzle: what the players are asked and what the correct solution is.
 * The solution never leaves the server.
 *
 * @param id stable puzzle identifier, for example {@code chest}
 * @param prompt the question shown to the players
 * @param solution the correct answer, checked on the server only
 */
public record PuzzleDefinition(String id, String prompt, String solution) {

    private static final Pattern ID = Pattern.compile("[a-z0-9-]{1,32}");

    /**
     * Validates the definition.
     */
    public PuzzleDefinition {
        if (id == null || !ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Puzzle id must be 1-32 characters of a-z, 0-9 or -");
        }
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Puzzle prompt must not be blank");
        }
        if (solution == null || solution.isBlank()) {
            throw new IllegalArgumentException("Puzzle solution must not be blank");
        }
    }
}
