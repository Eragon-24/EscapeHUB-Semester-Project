package com.escapehub.gameplay.domain;

/**
 * Result of submitting an answer to a session puzzle.
 *
 * @param correct whether the submitted answer solved the puzzle
 * @param message feedback for the participant
 * @param gameState authoritative state after the submission
 */
public record PuzzleSubmissionResult(boolean correct, String message, GameState gameState) {
}
