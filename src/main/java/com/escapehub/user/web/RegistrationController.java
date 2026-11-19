package com.escapehub.user.web;

import com.escapehub.user.application.DuplicateUserException;
import com.escapehub.user.application.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * US-007: σελίδα εγγραφής χρήστη.
 */
@Controller
public class RegistrationController {

    private final RegistrationService registrationService;

    /**
     * Δημιουργεί τον controller.
     *
     * @param registrationService η υπηρεσία εγγραφής
     */
    public RegistrationController(final RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    /**
     * Εμφανίζει την κενή φόρμα εγγραφής.
     *
     * @param model το μοντέλο
     * @return το template της φόρμας
     */
    @GetMapping("/register")
    public String showForm(final Model model) {
        model.addAttribute("form", new RegistrationForm());
        return "register";
    }

    /**
     * Επεξεργάζεται την εγγραφή. Μη έγκυρα ή διπλότυπα στοιχεία επιστρέφουν στη φόρμα με μήνυμα.
     *
     * @param form τα στοιχεία της φόρμας
     * @param bindingResult το αποτέλεσμα του validation
     * @param model το μοντέλο
     * @return η φόρμα σε σφάλμα, αλλιώς το template επιτυχίας
     */
    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("form") final RegistrationForm form,
            final BindingResult bindingResult,
            final Model model) {
        if (bindingResult.hasErrors()) {
            return "register";
        }
        try {
            registrationService.register(form.getUsername(), form.getEmail(), form.getPassword());
        } catch (DuplicateUserException e) {
            final String message = "email".equals(e.getField())
                    ? "Υπάρχει ήδη λογαριασμός με αυτό το email."
                    : "Το όνομα χρήστη χρησιμοποιείται ήδη.";
            bindingResult.rejectValue(e.getField(), "duplicate", message);
            return "register";
        }
        model.addAttribute("username", form.getUsername().trim());
        return "register-success";
    }
}
