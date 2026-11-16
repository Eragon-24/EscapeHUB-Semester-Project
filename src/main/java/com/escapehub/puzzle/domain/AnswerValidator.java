package com.escapehub.puzzle.domain;

public class AnswerValidator {

    public boolean validate(String userAnswer, String correctAnswer) {
        if (userAnswer == null || correctAnswer == null) {
            return false;
        }
        return userAnswer.trim().equalsIgnoreCase(correctAnswer.trim());
    }
}