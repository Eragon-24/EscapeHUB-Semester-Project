package com.escapehub.gameplay.domain;

import com.escapehub.puzzle.domain.AnswerValidator;
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
}