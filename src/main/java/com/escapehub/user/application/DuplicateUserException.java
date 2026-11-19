package com.escapehub.user.application;

/**
 * US-007: ο λογαριασμός δεν δημιουργήθηκε επειδή το username ή το email υπάρχει ήδη.
 */
public class DuplicateUserException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String field;

    /**
     * Δημιουργεί το σφάλμα για το πεδίο που είναι διπλότυπο.
     *
     * @param field "username" ή "email"
     */
    public DuplicateUserException(final String field) {
        super("Duplicate " + field);
        this.field = field;
    }

    /**
     * Επιστρέφει το πεδίο που είναι διπλότυπο.
     *
     * @return "username" ή "email"
     */
    public String getField() {
        return field;
    }
}
