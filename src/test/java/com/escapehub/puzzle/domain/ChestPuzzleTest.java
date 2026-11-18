package com.escapehub.puzzle.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ChestPuzzleTest {

    @Test
    void correctCodeSolvesPuzzleAndShowsSuccess() {
        ChestPuzzle puzzle = new ChestPuzzle("483920");
        AnswerResult result = puzzle.check(" 483920 ");
        assertTrue(result.correct());
        assertEquals(AnswerResult.SUCCESS_MESSAGE, result.message());
        assertTrue(puzzle.isSolved());
    }

    @Test
    void wrongCodeShowsFailureAndKeepsPuzzleUnsolved() {
        ChestPuzzle puzzle = new ChestPuzzle("483920");
        AnswerResult result = puzzle.check("000000");
        assertFalse(result.correct());
        assertEquals(AnswerResult.FAILURE_MESSAGE, result.message());
        assertFalse(puzzle.isSolved());
    }

    @Test
    void wrongAttemptAfterSolvedDoesNotUnsolve() {
        ChestPuzzle puzzle = new ChestPuzzle("483920");
        puzzle.check("483920");
        puzzle.check("000000");
        assertTrue(puzzle.isSolved());
    }
}
