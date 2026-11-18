package com.escapehub.user.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * US-007 + US-008: ένας λογαριασμός που δημιουργείται με την εγγραφή μπορεί να συνδεθεί, με τα πραγματικά
 * beans της εφαρμογής (ίδιος PasswordEncoder για εγγραφή και σύνδεση).
 */
@SpringBootTest
class RegisteredUserLoginTest {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private DaoAuthenticationProvider provider() {
        final DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Test
    void registeredUserCanAuthenticateWithTheirPassword() {
        registrationService.register("login_user", "login@example.com", "secret123");

        final Authentication result = provider().authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("login_user", "secret123"));

        assertTrue(result.isAuthenticated());
        assertEquals("login_user", result.getName());
    }

    @Test
    void usernameMatchesIgnoringCaseAtLogin() {
        registrationService.register("CaseUser", "case@example.com", "secret123");

        final Authentication result = provider().authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("caseuser", "secret123"));

        assertTrue(result.isAuthenticated());
    }

    @Test
    void wrongPasswordIsRejected() {
        registrationService.register("wrong_pw", "wrong@example.com", "secret123");

        assertThrows(BadCredentialsException.class, () -> provider().authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("wrong_pw", "not-the-password")));
    }

    @Test
    void unknownUserIsRejected() {
        assertThrows(BadCredentialsException.class, () -> provider().authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("ghost_user", "secret123")));
    }
}
