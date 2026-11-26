package com.escapehub.puzzle.domain;

import java.util.regex.Pattern;

/**
 * US-022: the configured definition of a puzzle: its display title, prompt and server-side solution.
 * The solution never leaves the server.
 *
 * @param id stable puzzle identifier, for example {@code chest}
 * @param title player-facing puzzle title
 * @param prompt the question shown to the players
 * @param solution the correct answer, checked on the server only
 */
public record PuzzleDefinition(String id, String title, String prompt, String solution) {

    private static final Pattern ID = Pattern.compile("[a-z0-9-]{1,32}");

    /**
     * Backward-compatible constructor for existing catalog definitions.
     */
    public PuzzleDefinition(final String id, final String prompt, final String solution) {
        this(id, id, prompt, solution);
    }

    /**
     * Validates the definition.
     */
    public PuzzleDefinition {
        if (id == null || !ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Puzzle id must be 1-32 characters of a-z, 0-9 or -");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Puzzle title must not be blank");
        }
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Puzzle prompt must not be blank");
        }
        if (solution == null || solution.isBlank()) {
            throw new IllegalArgumentException("Puzzle solution must not be blank");
        }
        title = title.trim();
    }
}
