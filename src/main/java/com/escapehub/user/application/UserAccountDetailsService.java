package com.escapehub.user.application;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * US-007/US-008: φορτώνει τους εγγεγραμμένους χρήστες για το Spring Security, ώστε ένας λογαριασμός
 * που δημιουργήθηκε με την εγγραφή να μπορεί να συνδεθεί. Χρησιμοποιεί το ίδιο {@code PasswordEncoder}
 * με την εγγραφή, επομένως ο αποθηκευμένος hash επαληθεύεται σωστά κατά τη σύνδεση.
 */
@Service
public class UserAccountDetailsService implements UserDetailsService {

    private final UserRepository users;

    /**
     * Δημιουργεί την υπηρεσία.
     *
     * @param users το repository χρηστών
     */
    public UserAccountDetailsService(final UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
        if (username == null) {
            throw new UsernameNotFoundException("Unknown user");
        }
        return users.findByUsername(username.trim())
                .map(user -> User.withUsername(user.username())
                        .password(user.passwordHash())
                        .roles("PLAYER")
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
    }
}
