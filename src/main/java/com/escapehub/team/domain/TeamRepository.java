package com.escapehub.team.domain;

import java.util.Optional;

/**
 * Η σύνδεση με την αποθήκευση. Σήμερα την υλοποιεί το FileTeamRepository (txt),
 * αργότερα θα την υλοποιεί η βάση δεδομένων.
 */
public interface TeamRepository {

    /**
     * Αποθηκεύει την ομάδα και τα μέλη της και την επιστρέφει.
     * Αν η ομάδα δεν έχει id, δημιουργείται νέα και παίρνει id.
     * Αν έχει id, ενημερώνεται η υπάρχουσα (π.χ. μετά από addMember).
     */
    Team save(Team team);

    /** True αν υπάρχει ήδη ομάδα με αυτόν τον κωδικό συμμετοχής. */
    boolean existsByJoinCode(String joinCode);

    /** Βρίσκει την ομάδα με αυτόν τον κωδικό συμμετοχής, αν υπάρχει. */
    Optional<Team> findByJoinCode(String joinCode);
}
