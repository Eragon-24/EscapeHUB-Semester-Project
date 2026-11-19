package com.escapehub.gameplay.application;

import com.escapehub.gameplay.domain.MissionRun;
import java.util.List;
import java.util.Optional;

/**
 * US-015: θύρα αποθήκευσης των MissionRun. Η υλοποίηση (in-memory τώρα, βάση αργότερα) ζει στο infrastructure.
 */
public interface MissionRunRepository {

    /**
     * Αποδίδει το επόμενο διαθέσιμο id.
     *
     * @return νέο μοναδικό id
     */
    long nextId();

    /**
     * Αποθηκεύει (δημιουργεί ή αντικαθιστά) μια εκτέλεση.
     *
     * @param run η εκτέλεση
     * @return η αποθηκευμένη εκτέλεση
     */
    MissionRun save(MissionRun run);

    /**
     * Βρίσκει εκτέλεση με id.
     *
     * @param id το id
     * @return η εκτέλεση, αν υπάρχει
     */
    Optional<MissionRun> findById(long id);

    /**
     * Επιστρέφει όλες τις εκτελέσεις ενός παίκτη (χωρίς διάκριση πεζών/κεφαλαίων στο username).
     *
     * @param ownerUsername ο παίκτης
     * @return οι εκτελέσεις του
     */
    List<MissionRun> findByOwner(String ownerUsername);
}
