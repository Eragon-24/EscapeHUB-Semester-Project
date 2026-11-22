package com.escapehub.team.web;

import com.escapehub.team.application.LeaveTeamService;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * US-014: το κουμπί «Leave Session». Ο χρήστης προσδιορίζεται ΜΟΝΟ από τον συνδεδεμένο
 * χρήστη (Principal), ποτέ από παράμετρο του αιτήματος.
 */
@Controller
@RequestMapping("/multiplayer")
public class LeaveTeamController {

    /** Η σελίδα Multiplayer Room, όπου επιστρέφει ο παίκτης. Να ταιριάζει με το πραγματικό URL. */
    public static final String MULTIPLAYER_ROOM_PATH = "/multiplayer";

    private final LeaveTeamService leaveTeamService;

    public LeaveTeamController(LeaveTeamService leaveTeamService) {
        this.leaveTeamService = leaveTeamService;
    }

    @PostMapping("/teams/{joinCode}/leave")
    public String leave(@PathVariable String joinCode, Principal principal,
                        RedirectAttributes redirectAttributes) {
        long currentUserId = currentUserId(principal);
        LeaveTeamService.LeaveResult result = leaveTeamService.leaveTeam(currentUserId, joinCode);
        redirectAttributes.addFlashAttribute("message", result.message());
        return "redirect:" + MULTIPLAYER_ROOM_PATH;
    }

    /**
     * Το ΜΟΝΟ σημείο που διαβάζει τον συνδεδεμένο χρήστη. Εδώ υποθέτουμε ότι το όνομα του
     * Principal είναι το numeric id του χρήστη. Όταν υλοποιηθεί το user/ module, αλλάζει
     * μόνο αυτή η μέθοδος.
     */
    private long currentUserId(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        }
        try {
            return Long.parseLong(principal.getName());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user");
        }
    }
}