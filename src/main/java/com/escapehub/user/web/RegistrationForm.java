package com.escapehub.user.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * US-007: δεδομένα της φόρμας εγγραφής.
 */
public class RegistrationForm {

    @NotBlank(message = "Το όνομα χρήστη είναι υποχρεωτικό.")
    @Pattern(regexp = "[A-Za-z0-9_]{3,30}",
            message = "3-30 χαρακτήρες: λατινικά γράμματα, αριθμοί ή _.")
    private String username;

    @NotBlank(message = "Το email είναι υποχρεωτικό.")
    @Email(message = "Δώσε έγκυρο email.")
    private String email;

    @NotBlank(message = "Ο κωδικός είναι υποχρεωτικός.")
    @Size(min = 8, message = "Ο κωδικός πρέπει να έχει τουλάχιστον 8 χαρακτήρες.")
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }
}
