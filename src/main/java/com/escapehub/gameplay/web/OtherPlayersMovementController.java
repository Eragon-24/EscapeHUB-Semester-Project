package com.escapehub.gameplay.web;

import com.escapehub.gameplay.application.OtherPlayersMovementService;
import com.escapehub.gameplay.domain.movement.OtherPlayerView;
import com.escapehub.gameplay.domain.movement.PlayerPosition;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for US-017 updating a player's position and viewing other players' movements.
 */
@RestController
public class OtherPlayersMovementController {

    private final OtherPlayersMovementService otherPlayersMovementService;

    /**
     * Creates the controller for viewing and updating player movements.
     *
     * @param otherPlayersMovementService application service for player positions
     */
    public OtherPlayersMovementController(
            final OtherPlayersMovementService otherPlayersMovementService) {
        this.otherPlayersMovementService = otherPlayersMovementService;
    }

    /**
     * Returns positions of all other players in the given session for the authenticated member.
     *
     * @param sessionId unique session identifier
     * @param principal authenticated user
     * @return 200 OK with other players' positions, or an appropriate error status
     */
    @GetMapping("/api/v1/game-sessions/{sessionId}/other-players")
    public ResponseEntity<?> getOtherPlayers(
            @PathVariable("sessionId") final UUID sessionId,
            final Principal principal) {
        if (!isAuthenticated(principal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            final List<OtherPlayerView> others = otherPlayersMovementService
                    .getOtherPlayersPositions(sessionId, principal.getName());
            return ResponseEntity.ok(others);
        } catch (final RuntimeException exception) {
            return mapException(exception);
        }
    }

    /**
     * Updates the authenticated participant's current room coordinates.
     *
     * @param sessionId unique session identifier
     * @param request coordinates payload
     * @param principal authenticated user
     * @return 200 OK with the saved position, or an appropriate error status
     */
    @PutMapping("/api/v1/game-sessions/{sessionId}/player-position")
    public ResponseEntity<?> updatePlayerPosition(
            @PathVariable("sessionId") final UUID sessionId,
            @RequestBody final PositionUpdateRequest request,
            final Principal principal) {
        if (!isAuthenticated(principal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (request == null || request.x() == null || request.y() == null) {
            return error(HttpStatus.BAD_REQUEST, "Οι συντεταγμένες x και y είναι υποχρεωτικές.");
        }
        try {
            final PlayerPosition position = new PlayerPosition(request.x(), request.y());
            otherPlayersMovementService.updatePlayerPosition(
                    sessionId, principal.getName(), position);
            return ResponseEntity.ok(position);
        } catch (final RuntimeException exception) {
            return mapException(exception);
        }
    }

    /**
     * Removes the authenticated player's tracked position when leaving the room.
     *
     * @param sessionId unique session identifier
     * @param principal authenticated user
     * @return 204 No Content on success, or an appropriate error status
     */
    @DeleteMapping("/api/v1/game-sessions/{sessionId}/player-position")
    public ResponseEntity<?> removePlayerPosition(
            @PathVariable("sessionId") final UUID sessionId,
            final Principal principal) {
        if (!isAuthenticated(principal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            otherPlayersMovementService.removePlayer(sessionId, principal.getName());
            return ResponseEntity.noContent().build();
        } catch (final RuntimeException exception) {
            return mapException(exception);
        }
    }

    private ResponseEntity<?> mapException(final RuntimeException exception) {
        if (exception instanceof NoSuchElementException) {
            return error(HttpStatus.NOT_FOUND, exception.getMessage());
        }
        if (exception instanceof SecurityException) {
            return error(HttpStatus.FORBIDDEN, exception.getMessage());
        }
        if (exception instanceof IllegalStateException) {
            return error(HttpStatus.CONFLICT, exception.getMessage());
        }
        if (exception instanceof IllegalArgumentException) {
            return error(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
        throw exception;
    }

    private ResponseEntity<?> error(
            final HttpStatus status, final String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }

    private boolean isAuthenticated(final Principal principal) {
        return principal != null && !"anonymousUser".equals(principal.getName());
    }

    /**
     * Request payload for updating a player's room coordinates.
     *
     * @param x horizontal coordinate
     * @param y vertical coordinate
     */
    public record PositionUpdateRequest(Double x, Double y) {
    }
}