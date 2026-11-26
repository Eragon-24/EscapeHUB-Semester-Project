package com.escapehub.puzzle.domain;

import java.util.List;
import java.util.UUID;

/** Immutable snapshot of the common puzzle progress for one game session. */
public record CommonPuzzleProgressView(
        UUID sessionId,
        String roomId,
        int totalPuzzles,
        int solvedCount,
        int remainingCount,
        int completionPercentage,
        long stateVersion,
        List<PuzzleProgressItem> puzzles) {
    public CommonPuzzleProgressView {
        if (sessionId == null || roomId == null || roomId.isBlank() || puzzles == null) {
            throw new IllegalArgumentException("Progress snapshot fields must not be null");
        }
        puzzles = List.copyOf(puzzles);
    }
}
