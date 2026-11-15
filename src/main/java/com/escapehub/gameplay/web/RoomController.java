package com.escapehub.gameplay.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for displaying the initial demo room (US-001).
 */
@Controller
public class RoomController {

    /**
     * Displays the abandoned laboratory room with its initial selectable object.
     *
     * @param model the Spring MVC model
     * @return the room template name
     */
    @GetMapping("/room")
    public String showRoom(final Model model) {
        model.addAttribute("roomName", "Εγκαταλελειμμένο Εργαστήριο");
        model.addAttribute(
                "roomDescription",
                " βρίσκεσαι σε ένα σκοτεινό, σκονισμένο εργαστήριο ερευνών. "
                        + "Πάνω στον κεντρικό πάγκο διακρίνεται ένα αντικείμενο που "
                        + "φαίνεται να κρύβει την πρώτη σου ένδειξη."
        );
        model.addAttribute("objectId", "lab-notebook");
        model.addAttribute("objectName", "Παλιό Σημειωματάριο Ερευνών");
        model.addAttribute(
                "objectSummary",
                "Δερματόδετο σημειωματάριο με χειρόγραφες σημειώσεις."
        );
        model.addAttribute(
                "objectClue",
                "Στην τελευταία σελίδα είναι γραμμένος ένας τετραψήφιος κωδικός: 4-7-1-9. "
                        + "Δίπλα του, υπογραμμισμένο: «Το ντουλάπι με τα αντιδραστήρια»."
        );
        return "room";
    }
}