package com.escapehub.gameplay.domain;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * US-022: which puzzles of one game session are solved, by whom and when. A puzzle can be solved only once, so its
 * effects cannot be duplicated.
 *
 * <p>The class is not thread-safe: callers must synchronize on the instance (the application service does).</p>
 */
public final class SessionPuzzleProgress {

    private final UUID sessionId;
    private final Map<String, Solution> solved = new LinkedHashMap<>();

    /**
     * Creates empty progress for a session.
     *
     * @param sessionId the game session
     */
    public SessionPuzzleProgress(final UUID sessionId) {
        this.sessionId = Objects.requireNonNull(sessionId, "Session id must not be null");
    }

    /**
     * Records a puzzle as solved, only if it was not solved before.
     *
     * @param puzzleId the puzzle
     * @param solvedBy username of the participant who solved it
     * @param at time of the solution
     * @return true if this call solved the puzzle, false if it was already solved (nothing changes)
     */
    public boolean markSolved(final String puzzleId, final String solvedBy, final Instant at) {
        if (solved.containsKey(puzzleId)) {
            return false;
        }
        solved.put(puzzleId, new Solution(solvedBy, at));
        return true;
    }

    /**
     * Tells whether a puzzle is solved in this session.
     *
     * @param puzzleId the puzzle
     * @return true if solved
     */
    public boolean isSolved(final String puzzleId) {
        return solved.containsKey(puzzleId);
    }

    /**
     * Returns the ids of the solved puzzles.
     *
     * @return an immutable view of the solved puzzle ids
     */
    public Set<String> solvedPuzzleIds() {
        return Collections.unmodifiableSet(solved.keySet());
    }

    /**
     * Returns the session this progress belongs to.
     *
     * @return the session id
     */
    public UUID getSessionId() {
        return sessionId;
    }

    /**
     * Who solved a puzzle and when.
     *
     * @param solvedBy username of the solver
     * @param at time of the solution
     */
    public record Solution(String solvedBy, Instant at) {
    }
}
