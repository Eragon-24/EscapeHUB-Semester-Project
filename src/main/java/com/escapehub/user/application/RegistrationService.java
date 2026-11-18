package com.escapehub.user.application;

import com.escapehub.user.domain.User;
import java.util.regex.Pattern;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * US-007: εγγραφή χρήστη. Έγκυρα στοιχεία δημιουργούν λογαριασμό· διπλότυπα ή μη έγκυρα απορρίπτονται.
 */
@Service
public class RegistrationService {

    /** Ελάχιστο μήκος κωδικού. */
    public static final int MIN_PASSWORD_LENGTH = 8;

    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_]{3,30}");
    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    /**
     * Δημιουργεί την υπηρεσία εγγραφής.
     *
     * @param users το repository χρηστών
     * @param passwordEncoder ο κωδικοποιητής κωδικών
     */
    public RegistrationService(final UserRepository users, final PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Εγγράφει νέο χρήστη.
     *
     * @param username όνομα χρήστη (3-30 χαρακτήρες: γράμματα, αριθμοί, _)
     * @param email έγκυρο email
     * @param password κωδικός τουλάχιστον 8 χαρακτήρων
     * @return ο νέος λογαριασμός
     * @throws IllegalArgumentException αν τα στοιχεία δεν είναι έγκυρα
     * @throws DuplicateUserException αν το username ή το email υπάρχει ήδη
     */
    public User register(final String username, final String email, final String password) {
        final String cleanUsername = username == null ? "" : username.trim();
        final String cleanEmail = email == null ? "" : email.trim();
        if (!USERNAME.matcher(cleanUsername).matches()) {
            throw new IllegalArgumentException("Invalid username");
        }
        if (!EMAIL.matcher(cleanEmail).matches()) {
            throw new IllegalArgumentException("Invalid email");
        }
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Invalid password");
        }
        if (users.findByUsername(cleanUsername).isPresent()) {
            throw new DuplicateUserException("username");
        }
        if (users.findByEmail(cleanEmail).isPresent()) {
            throw new DuplicateUserException("email");
        }
        return users.save(cleanUsername, cleanEmail, passwordEncoder.encode(password));
    }
}
