package com.escapehub.puzzle.domain;

/**
 * US-004: έλεγχος απάντησης παίκτη (αγνοεί κενά στις άκρες και πεζά/κεφαλαία).
 */
public class AnswerValidator {

    /**
     * Ελέγχει αν η απάντηση ταιριάζει με τη σωστή.
     *
     * @param userAnswer η απάντηση του παίκτη
     * @param correctAnswer η σωστή απάντηση
     * @return true αν ταιριάζουν, false αλλιώς (και όταν κάποια είναι null, κενή ή μόνο κενά)
     */
    public boolean validate(String userAnswer, String correctAnswer) {
        if (userAnswer == null || correctAnswer == null
                || userAnswer.isBlank() || correctAnswer.isBlank()) {
            return false;
        }
        return userAnswer.trim().equalsIgnoreCase(correctAnswer.trim());
    }

    /**
     * Ελέγχει την απάντηση και επιστρέφει αποτέλεσμα με μήνυμα επιτυχίας ή αποτυχίας.
     *
     * @param userAnswer η απάντηση του παίκτη
     * @param correctAnswer η σωστή απάντηση
     * @return το αποτέλεσμα με το μήνυμα ανατροφοδότησης
     */
    public AnswerResult check(String userAnswer, String correctAnswer) {
        return validate(userAnswer, correctAnswer) ? AnswerResult.success() : AnswerResult.failure();
    }
}
