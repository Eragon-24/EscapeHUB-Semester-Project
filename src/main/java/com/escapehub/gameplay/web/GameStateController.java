package com.escapehub.gameplay.web;

import com.escapehub.gameplay.application.GameStateService;
import com.escapehub.gameplay.domain.GameState;
import java.security.Principal;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<?> enterGame(
            @PathVariable final String joinCode, final Principal principal) {
        return respond(() -> gameStateService.enterGame(joinCode, principal.getName()));
    }

    @GetMapping("/game-sessions/{joinCode}/game")
    public ResponseEntity<?> getGame(
            @PathVariable final String joinCode, final Principal principal) {
        try {
            return gameStateService.getState(joinCode, principal.getName())
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> error(HttpStatus.NOT_FOUND, "Το παιχνίδι δεν έχει ξεκινήσει."));
        } catch (final RuntimeException exception) {
            return failure(exception);
        }
    }

    private ResponseEntity<?> respond(final java.util.function.Supplier<GameState> action) {
        try {
            return ResponseEntity.ok(action.get());
        } catch (final RuntimeException exception) {
            return failure(exception);
        }
    }

    private ResponseEntity<?> failure(final RuntimeException exception) {
        if (exception instanceof SecurityException) {
            return error(HttpStatus.FORBIDDEN, exception.getMessage());
        }
        if (exception instanceof IllegalStateException) {
            return error(HttpStatus.CONFLICT, exception.getMessage());
        }
        return error(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    private ResponseEntity<?> error(final HttpStatus status, final String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}