package com.escapehub.gameplay.application;

import com.escapehub.gameplay.domain.movement.OtherPlayerView;
import com.escapehub.gameplay.domain.movement.PlayerPosition;
import com.escapehub.gameplay.domain.movement.SessionPlayerPositions;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * US-017: Application service for recording player movements and querying
 * other players' current positions within a game session.
 */
@Service
public class OtherPlayersMovementService {

    private final SessionPlayerPositionsRepository repository;

    /**
     * Creates the service backed by a player-positions repository.
     *
     * @param repository storage port for session player positions
     */
    public OtherPlayersMovementService(final SessionPlayerPositionsRepository repository) {
        this.repository = Objects.requireNonNull(repository, "Repository must not be null");
    }

    /**
     * Records a player's updated position in the given session.
     *
     * @param sessionId unique session identifier
     * @param username player's username
     * @param position player's new room coordinates
     */
    public synchronized void updatePlayerPosition(
            final UUID sessionId,
            final String username,
            final PlayerPosition position) {
        Objects.requireNonNull(sessionId, "Session id must not be null");
        final SessionPlayerPositions tracked = repository.findBySessionId(sessionId)
                .orElseGet(() -> new SessionPlayerPositions(sessionId));
        tracked.updatePosition(username, position);
        repository.save(tracked);
    }

    /**
     * Removes a player from the session's visible players.
     *
     * @param sessionId unique session identifier
     * @param username player's username
     */
    public synchronized void removePlayer(final UUID sessionId, final String username) {
        Objects.requireNonNull(sessionId, "Session id must not be null");
        repository.findBySessionId(sessionId).ifPresent(tracked -> {
            tracked.removePlayer(username);
            repository.save(tracked);
        });
    }

    /**
     * Retrieves the latest positions of all other players in the session.
     *
     * @param sessionId unique session identifier
     * @param viewerUsername authenticated viewer's username
     * @return list of other players and their coordinates
     */
    public synchronized List<OtherPlayerView> getOtherPlayersPositions(
            final UUID sessionId,
            final String viewerUsername) {
        Objects.requireNonNull(sessionId, "Session id must not be null");
        if (viewerUsername == null || viewerUsername.isBlank()) {
            throw new IllegalArgumentException("Viewer username must not be blank");
        }
        return repository.findBySessionId(sessionId)
                .map(tracked -> tracked.getOtherPlayers(viewerUsername))
                .orElseGet(List::of);
    }
}