package com.escapehub.user.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.escapehub.user.domain.User;
import com.escapehub.user.infrastructure.InMemoryUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class RegistrationServiceTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private RegistrationService service;

    @BeforeEach
    void setUp() {
        service = new RegistrationService(new InMemoryUserRepository(), encoder);
    }

    @Test
    void validDetailsCreateAccountWithHashedPassword() {
        final User user = service.register("marios_1", "marios@example.com", "secret123");

        assertEquals("marios_1", user.username());
        assertEquals("marios@example.com", user.email());
        assertNotEquals("secret123", user.passwordHash());
        assertTrue(encoder.matches("secret123", user.passwordHash()));
    }

    @Test
    void duplicateUsernameIsRejectedIgnoringCase() {
        service.register("marios", "a@example.com", "secret123");

        final DuplicateUserException ex = assertThrows(DuplicateUserException.class,
                () -> service.register("MARIOS", "b@example.com", "secret123"));
        assertEquals("username", ex.getField());
    }

    @Test
    void duplicateEmailIsRejectedIgnoringCase() {
        service.register("marios", "a@example.com", "secret123");

        final DuplicateUserException ex = assertThrows(DuplicateUserException.class,
                () -> service.register("other", "A@Example.com", "secret123"));
        assertEquals("email", ex.getField());
    }

    @Test
    void invalidUsernameIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.register("ab", "a@example.com", "secret123"));
        assertThrows(IllegalArgumentException.class,
                () -> service.register("bad name!", "a@example.com", "secret123"));
        assertThrows(IllegalArgumentException.class,
                () -> service.register(null, "a@example.com", "secret123"));
    }

    @Test
    void invalidEmailIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.register("marios", "not-an-email", "secret123"));
        assertThrows(IllegalArgumentException.class,
                () -> service.register("marios", "", "secret123"));
        assertThrows(IllegalArgumentException.class,
                () -> service.register("marios", null, "secret123"));
    }

    @Test
    void shortOrMissingPasswordIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.register("marios", "a@example.com", "short"));
        assertThrows(IllegalArgumentException.class,
                () -> service.register("marios", "a@example.com", null));
    }

    @Test
    void rejectedRegistrationDoesNotCreateAccount() {
        assertThrows(IllegalArgumentException.class,
                () -> service.register("marios", "bad", "secret123"));

        // the username is still free, so a valid registration succeeds
        assertEquals("marios", service.register("marios", "a@example.com", "secret123").username());
    }
}
