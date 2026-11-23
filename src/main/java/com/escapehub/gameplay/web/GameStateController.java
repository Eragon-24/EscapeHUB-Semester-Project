package com.escapehub.gameplay.web;

import com.escapehub.gameplay.application.GameSessionNotFoundException;
import com.escapehub.gameplay.application.GameStateService;
import com.escapehub.gameplay.domain.GameState;
import java.security.Principal;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GameStateController {

    private final GameStateService gameStateService;

    public GameStateController(final GameStateService gameStateService) {
        this.gameStateService = gameStateService;
    }

    @PostMapping("/game-sessions/{joinCode}/game")
    public GameState enterGame(@PathVariable final String joinCode, final Principal principal) {
        return gameStateService.enterGame(joinCode, requireUser(principal));
    }

    @GetMapping("/game-sessions/{joinCode}/game")
    public GameState getGame(@PathVariable final String joinCode, final Principal principal) {
        return gameStateService.getState(joinCode, requireUser(principal))
                .orElseThrow(() -> new GameSessionNotFoundException(
                        "Το παιχνίδι δεν έχει ξεκινήσει."));
    }

    private String requireUser(final Principal principal) {
        if (principal == null || "anonymousUser".equals(principal.getName())) {
            throw new UnauthenticatedException();
        }
        return principal.getName();
    }

    @ExceptionHandler(UnauthenticatedException.class)
    ResponseEntity<Map<String, String>> unauthenticated() {
        return error(HttpStatus.UNAUTHORIZED, "Απαιτείται σύνδεση χρήστη.");
    }

    @ExceptionHandler(SecurityException.class)
    ResponseEntity<Map<String, String>> forbidden(final SecurityException exception) {
        return error(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler(GameSessionNotFoundException.class)
    ResponseEntity<Map<String, String>> notFound(final GameSessionNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> badRequest(final IllegalArgumentException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<Map<String, String>> conflict(final IllegalStateException exception) {
        return error(HttpStatus.CONFLICT, exception.getMessage());
    }

    private ResponseEntity<Map<String, String>> error(final HttpStatus status, final String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }

    private static final class UnauthenticatedException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}