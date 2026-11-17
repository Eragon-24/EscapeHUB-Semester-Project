package com.escapehub.puzzle.domain;

/**
 * US-003: παζλ με εξάψηφο κωδικό (εκφώνηση + έλεγχος απάντησης).
 *
 * <p>Χρήση:
 * <pre>
 * ChestPuzzle puzzle = new ChestPuzzle("483920"); // ο κωδικός που βρήκε ο παίκτης
 * puzzle.getPrompt();// εκφώνηση για εμφάνιση
 * puzzle.submit("483920");// true = σωστός, false = λάθος
 * puzzle.isSolved();// true όταν λυθεί (για το άνοιγμα του chest)
 * </pre>
 */
public class ChestPuzzle {

    public static final int CODE_LENGTH = 6;

    private static final String PROMPT =
            "Το chest έχει κλειδαριά με εξάψηφο κωδικό. Πληκτρολόγησε τον κωδικό.";

    private static final AnswerValidator VALIDATOR = new AnswerValidator();

    private final String code;
    private boolean solved;

    /** Ο σωστός κωδικός δίνεται απ' έξω (από το βήμα του χαρτιού). Πρέπει να είναι 6 ψηφία. */
    public ChestPuzzle(String code) {
        if (code == null || !code.matches("\\d{" + CODE_LENGTH + "}")) {
            throw new IllegalArgumentException("Code must be exactly " + CODE_LENGTH + " digits");
        }
        this.code = code;
    }

    /** Η εκφώνηση του παζλ. */
    public String getPrompt() {
        return PROMPT;
    }

    /** Πόσα ψηφία έχει ο κωδικός (για το πεδίο εισαγωγής). */
    public int getCodeLength() {
        return CODE_LENGTH;
    }

    /** US-004: ελέγχει την απάντηση και επιστρέφει αποτέλεσμα με μήνυμα επιτυχίας/αποτυχίας. */
    public AnswerResult check(String answer) {
        AnswerResult result = VALIDATOR.check(answer, code);
        if (result.correct()) {
            solved = true;
        }
        return result;
    }

    /** Υποβάλλει απάντηση. Επιστρέφει true αν το παζλ είναι λυμένο. */
    public boolean submit(String answer) {
        check(answer);
        return solved;
    }

    /** True αν το παζλ έχει λυθεί. */
    public boolean isSolved() {
        return solved;
    }
}

