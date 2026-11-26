package com.escapehub.puzzle.domain;

/** Progress status for one puzzle in the shared session. */
public record PuzzleProgressItem(String puzzleId, String title, String description, PuzzleStatus status,
                                 boolean solved) {
    public PuzzleProgressItem {
        if (puzzleId == null || puzzleId.isBlank() || title == null || title.isBlank()
                || description == null || description.isBlank() || status == null) {
            throw new IllegalArgumentException("Puzzle progress fields must be valid");
        }
        if (solved != (status == PuzzleStatus.SOLVED)) {
            throw new IllegalArgumentException("Solved flag must match puzzle status");
        }
    }
}