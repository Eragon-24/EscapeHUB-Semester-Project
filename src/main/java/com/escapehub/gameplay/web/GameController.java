package com.escapehub.gameplay.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Serves the browser-based multiplayer room. */
@Controller
public class GameController {

    @GetMapping("/game")
    public String showGame() {
        return "game";
    }
}
