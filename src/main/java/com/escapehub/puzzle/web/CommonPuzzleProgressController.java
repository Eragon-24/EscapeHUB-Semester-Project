package com.escapehub.puzzle.web;

import com.escapehub.puzzle.application.CommonPuzzleProgressService;
import com.escapehub.puzzle.domain.CommonPuzzleProgressView;
import java.security.Principal;
import java.util.Map;
import com.escapehub.gameplay.application.GameSessionNotFoundException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoint for viewing the common puzzle progress of a session. */
@RestController
public class CommonPuzzleProgressController {
    private final CommonPuzzleProgressService service;

    public CommonPuzzleProgressController(final CommonPuzzleProgressService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/game-sessions/{sessionId}/puzzle-progress")
    public ResponseEntity<?> getProgress(@PathVariable final UUID sessionId, final Principal principal) {
        if (principal == null || "anonymousUser".equals(principal.getName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            final CommonPuzzleProgressView progress = service.getCommonProgress(sessionId, principal.getName());
            return ResponseEntity.ok(progress);
        } catch (final GameSessionNotFoundException exception) {
            return error(HttpStatus.NOT_FOUND, exception.getMessage());
        } catch (final SecurityException exception) {
            return error(HttpStatus.FORBIDDEN, exception.getMessage());
        } catch (final IllegalStateException exception) {
            return error(HttpStatus.CONFLICT, exception.getMessage());
        } catch (final IllegalArgumentException exception) {
            return error(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }

    private ResponseEntity<Map<String, String>> error(final HttpStatus status, final String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}
