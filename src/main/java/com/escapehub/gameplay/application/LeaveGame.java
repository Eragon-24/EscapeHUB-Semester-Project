package com.escapehub.gameplay.application;

import org.springframework.stereotype.Service;

/**
 * US-014: ο παίκτης αποχωρεί από το παιχνίδι και επιστρέφει στο αρχικό μενού.
 *
 * <p>Η κλάση έχει μόνο το σημείο εισόδου και την επιστροφή στο μενού. Η πραγματική
 * αφαίρεση του παίκτη από τη συνεδρία (GameSession) ΔΕΝ γίνεται εδώ ακόμη: υπάρχει μία
 * κενή μέθοδος, {@link #removeFromSession}, που τη συμπληρώνει όποιος αναλάβει το
 * GameSession.
 *
 * <p>Χρήση (ο currentUserId να είναι ο συνδεδεμένος χρήστης, ΠΟΤΕ από το αίτημα του client):
 * <pre>
 * LeaveResult result = leaveGame.leave(currentUserId, sessionId);
 * // redirect στο result.redirectTo() και εμφάνιση του result.message()
 * </pre>
 */
@Service
public class LeaveGame {

    /** Η σελίδα στην οποία επιστρέφει ο παίκτης. Αλλάζει όταν υπάρξει Multiplayer Room. */
    public static final String MAIN_MENU_PATH = "/";

    /** Πού να πάει ο παίκτης και τι μήνυμα να δει. */
    public record LeaveResult(String redirectTo, String message) { }

    /** Αποχωρεί ο παίκτης που κάνει το αίτημα και τον στέλνει στο αρχικό μενού. */
    public LeaveResult leave(long currentUserId, long sessionId) {
        removeFromSession(currentUserId, sessionId);
        return new LeaveResult(MAIN_MENU_PATH, "Αποχώρησες από το παιχνίδι.");
    }

    /**
     * ΚΕΝΗ ΕΠΙΤΡΟΠΗ ΘΕΣΗ για τον υπεύθυνο του GameSession.
     *
     * <p>Εδώ μπαίνει ο κώδικας που αφαιρεί ΜΟΝΟ τον χρήστη userId από τη συνεδρία sessionId,
     * ελευθερώνει τη θέση του και αφήνει ανέγγιχτα τη συνεδρία, το Game state και τους
     * υπόλοιπους παίκτες. Αν ο χρήστης δεν ανήκει στη συνεδρία, δεν πρέπει να αλλάξει τίποτα.
     */
    protected void removeFromSession(long userId, long sessionId) {
        // Σκόπιμα κενό μέχρι να υπάρξει GameSession.
    }
}