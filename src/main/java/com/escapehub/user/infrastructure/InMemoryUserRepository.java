package com.escapehub.user.infrastructure;

import com.escapehub.user.application.UserRepository;
import com.escapehub.user.domain.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * US-007: προσωρινή αποθήκευση χρηστών στη μνήμη. Θα αντικατασταθεί από βάση δεδομένων (persistence, W8).
 */
@Repository
public class InMemoryUserRepository implements UserRepository {

    private final List<User> users = new ArrayList<>();
    private long nextId = 1;

    @Override
    public synchronized User save(final String username, final String email, final String passwordHash) {
        final User user = new User(nextId++, username, email, passwordHash);
        users.add(user);
        return user;
    }

    @Override
    public synchronized Optional<User> findByUsername(final String username) {
        final String key = username.toLowerCase(Locale.ROOT);
        return users.stream().filter(u -> u.username().toLowerCase(Locale.ROOT).equals(key)).findFirst();
    }

    @Override
    public synchronized Optional<User> findByEmail(final String email) {
        final String key = email.toLowerCase(Locale.ROOT);
        return users.stream().filter(u -> u.email().toLowerCase(Locale.ROOT).equals(key)).findFirst();
    }
}
