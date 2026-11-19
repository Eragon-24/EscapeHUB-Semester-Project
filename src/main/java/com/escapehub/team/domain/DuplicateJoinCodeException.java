package com.escapehub.team.domain;

/**
 * Πετιέται όταν γίνεται προσπάθεια αποθήκευσης ομάδας με κωδικό συμμετοχής
 * που χρησιμοποιεί ήδη άλλη ομάδα. Κάθε υλοποίηση του TeamRepository
 * (txt, βάση δεδομένων) πρέπει να τη χρησιμοποιεί.
 */
public class DuplicateJoinCodeException extends RuntimeException {

    public DuplicateJoinCodeException(String joinCode) {
        super("Join code already in use: " + joinCode);
    }
}