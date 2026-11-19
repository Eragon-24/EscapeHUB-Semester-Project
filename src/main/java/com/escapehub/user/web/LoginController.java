package com.escapehub.user.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * US-008: εμφανίζει τη σελίδα σύνδεσης. Την επαλήθευση κάνει το Spring Security.
 */
@Controller
public class LoginController {

    /**
     * Σελίδα σύνδεσης.
     *
     * @return το όνομα του template σύνδεσης
     */
    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
