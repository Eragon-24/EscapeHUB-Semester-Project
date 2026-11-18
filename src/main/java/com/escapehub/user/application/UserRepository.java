package com.escapehub.user.application;

import com.escapehub.user.domain.User;
import java.util.Optional;

/**
 * US-007: θύρα αποθήκευσης χρηστών. Η υλοποίηση (in-memory τώρα, βάση αργότερα) ζει στο infrastructure.
 */
public interface UserRepository {

    /**
     * Αποθηκεύει νέο χρήστη και του αποδίδει id. Η μοναδικότητα του username και του email
     * (χωρίς διάκριση πεζών/κεφαλαίων) ελέγχεται και επιβάλλεται ατομικά εδώ.
     *
     * @param username όνομα χρήστη
     * @param email email
     * @param passwordHash hash κωδικού
     * @return ο αποθηκευμένος χρήστης
     * @throws DuplicateUserException αν το username ή το email υπάρχει ήδη
     */
    User save(String username, String email, String passwordHash);

    /**
     * Βρίσκει χρήστη με όνομα χρήστη (χωρίς διάκριση πεζών/κεφαλαίων).
     *
     * @param username όνομα χρήστη
     * @return ο χρήστης, αν υπάρχει
     */
    Optional<User> findByUsername(String username);

    /**
     * Βρίσκει χρήστη με email (χωρίς διάκριση πεζών/κεφαλαίων).
     *
     * @param email email
     * @return ο χρήστης, αν υπάρχει
     */
    Optional<User> findByEmail(String email);
}
