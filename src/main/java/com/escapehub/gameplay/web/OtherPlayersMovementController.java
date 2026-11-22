package com.escapehub.gameplay.web;

import com.escapehub.gameplay.application.OtherPlayersMovementService;
import com.escapehub.gameplay.domain.movement.OtherPlayerView;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoint for US-017 returning other players' current positions in a session.
 */
@RestController
public class OtherPlayersMovementController {

    private final OtherPlayersMovementService otherPlayersMovementService;

    /**
     * Creates the controller for viewing other players' movements.
     *
     * @param otherPlayersMovementService application service for player positions
     */
    public OtherPlayersMovementController(
            final OtherPlayersMovementService otherPlayersMovementService) {
        this.otherPlayersMovementService = otherPlayersMovementService;
    }

    /**
     * Returns positions of all other players in the given session for the authenticated user.
     *
     * @param sessionId unique session identifier
     * @param principal authenticated user
     * @return 200 OK with other players' positions, or 401 when unauthenticated
     */
    @GetMapping("/api/v1/game-sessions/{sessionId}/other-players")
    public ResponseEntity<List<OtherPlayerView>> getOtherPlayers(
            @PathVariable("sessionId") final UUID sessionId,
            final Principal principal) {
        if (!isAuthenticated(principal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        final List<OtherPlayerView> others = otherPlayersMovementService
                .getOtherPlayersPositions(sessionId, principal.getName());
        return ResponseEntity.ok(others);
    }

    private boolean isAuthenticated(final Principal principal) {
        return principal != null && !"anonymousUser".equals(principal.getName());
    }
}