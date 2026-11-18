package com.escapehub.gameplay.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Redirects the legacy room URL to the browser-based multiplayer demo.
 */
@Controller
public class RoomController {

    /**
     * Keeps the original room URL working while serving the new browser game.
     *
     * @return redirect to the browser-based game
     */
    @GetMapping("/room")
    public String showRoom() {
        return "redirect:/game";
    }
}
