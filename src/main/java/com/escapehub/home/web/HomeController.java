package com.escapehub.home.web;

import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    /**
     * Αρχική σελίδα. Αν ο χρήστης είναι συνδεδεμένος, περνάει το όνομά του στο template (US-008).
     *
     * @param principal ο συνδεδεμένος χρήστης ή null
     * @param model το Spring MVC model
     * @return το όνομα του template
     */
    @GetMapping("/")
    public String home(final Principal principal, final Model model) {
        model.addAttribute("username", principal == null ? null : principal.getName());
        return "index";
    }
}
