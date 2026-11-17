package com.escapehub.puzzle.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnswerValidatorTest {

    private AnswerValidator validator;

    @BeforeEach
    void setUp() {
        validator = new AnswerValidator();
    }

    @Test
    void shouldReturnTrueWhenAnswersMatchIgnoringCaseAndSpaces() {
        assertTrue(validator.validate("  SecretCode ", "secretcode"));
    }

    @Test
    void shouldReturnFalseWhenAnswersDoNotMatch() {
        assertFalse(validator.validate("wrong", "secretcode"));
    }

    @Test
    void shouldReturnFalseWhenInputIsNull() {
        assertFalse(validator.validate(null, "secretcode"));
        assertFalse(validator.validate("secretcode", null));
    }

    @Test
    void checkShouldReturnSuccessMessageForCorrectAnswer() {
        AnswerResult result = validator.check("483920", "483920");
        assertTrue(result.correct());
        assertEquals(AnswerResult.SUCCESS_MESSAGE, result.message());
    }

    @Test
    void checkShouldReturnFailureMessageForWrongAnswer() {
        AnswerResult result = validator.check("111111", "483920");
        assertFalse(result.correct());
        assertEquals(AnswerResult.FAILURE_MESSAGE, result.message());
    }
}
