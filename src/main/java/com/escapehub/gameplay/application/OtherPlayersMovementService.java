package com.escapehub.gameplay.application;

import com.escapehub.gameplay.domain.GameSession;
import com.escapehub.gameplay.domain.SessionParticipants;
import com.escapehub.gameplay.domain.movement.OtherPlayerView;
import com.escapehub.gameplay.domain.movement.PlayerPosition;
import com.escapehub.gameplay.domain.movement.RoomMovementValidator;
import com.escapehub.gameplay.domain.movement.SessionPlayerPositions;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * US-017: Application service for recording validated player movements and querying
 * other players' current positions within an active game session.
 */
@Service
public class OtherPlayersMovementService {

    private final SessionPlayerPositionsRepository repository;
    private final GameSessionRepository gameSessionRepository;
    private final SessionParticipantsRepository participantsRepository;
    private final RoomMovementValidator movementValidator;

    /**
     * Creates the service with the default laboratory movement rules.
     *
     * @param repository storage port for session player positions
     * @param gameSessionRepository repository for game sessions
     * @param participantsRepository repository for session participants
     */
    @Autowired
    public OtherPlayersMovementService(
            final SessionPlayerPositionsRepository repository,
            final GameSessionRepository gameSessionRepository,
            final SessionParticipantsRepository participantsRepository) {
        this(
                repository,
                gameSessionRepository,
                participantsRepository,
                RoomMovementValidator.defaultLaboratory()
        );
    }

    /**
     * Creates the service with a custom movement validator.
     *
     * @param repository storage port for session player positions
     * @param gameSessionRepository repository for game sessions
     * @param participantsRepository repository for session participants
     * @param movementValidator validator enforcing room bounds and obstacles
     */
    public OtherPlayersMovementService(
            final SessionPlayerPositionsRepository repository,
            final GameSessionRepository gameSessionRepository,
            final SessionParticipantsRepository participantsRepository,
            final RoomMovementValidator movementValidator) {
        this.repository = Objects.requireNonNull(repository, "Repository must not be null");
        this.gameSessionRepository = Objects.requireNonNull(
                gameSessionRepository, "Game session repository must not be null");
        this.participantsRepository = Objects.requireNonNull(
                participantsRepository, "Participants repository must not be null");
        this.movementValidator = Objects.requireNonNull(
                movementValidator, "Movement validator must not be null");
    }

    /**
     * Records a participant's updated position after enforcing US-016 bounds and obstacles.
     *
     * @param sessionId unique session identifier
     * @param username authenticated player's username
     * @param position player's new room coordinates
     */
    public synchronized void updatePlayerPosition(
            final UUID sessionId,
            final String username,
            final PlayerPosition position) {
        final Set<String> activePlayers = requireActiveParticipant(sessionId, username);
        movementValidator.validate(position);
        final SessionPlayerPositions tracked = repository.findBySessionId(sessionId)
                .orElseGet(() -> new SessionPlayerPositions(sessionId));
        tracked.updatePosition(username, position);
        repository.save(tracked);
    }

    /**
     * Removes a participant from the session's visible players.
     *
     * @param sessionId unique session identifier
     * @param username authenticated participant's username
     */
    public synchronized void removePlayer(final UUID sessionId, final String username) {
        requireActiveParticipant(sessionId, username);
        repository.findBySessionId(sessionId).ifPresent(tracked -> {
            tracked.removePlayer(username);
            repository.save(tracked);
        });
    }

    /**
     * Retrieves the latest positions of all other active players in the session.
     *
     * @param sessionId unique session identifier
     * @param viewerUsername authenticated viewer's username
     * @return list of other players and their coordinates
     */
    public synchronized List<OtherPlayerView> getOtherPlayersPositions(
            final UUID sessionId,
            final String viewerUsername) {
        final Set<String> activePlayers = requireActiveParticipant(sessionId, viewerUsername);
        final Optional<SessionPlayerPositions> trackedOptional =
                repository.findBySessionId(sessionId);
        if (trackedOptional.isEmpty()) {
            return List.of();
        }
        final SessionPlayerPositions tracked = trackedOptional.get();
        return tracked.getOtherPlayers(viewerUsername, activePlayers);
    }

    private GameSession requireExistingSession(final UUID sessionId) {
        Objects.requireNonNull(sessionId, "Session id must not be null");
        return gameSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Η συνεδρία δεν βρέθηκε."));
    }

    private Set<String> requireActiveParticipant(final UUID sessionId, final String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Απαιτείται σύνδεση χρήστη.");
        }
        final GameSession session = requireExistingSession(sessionId);
        final String cleanUsername = username.trim();
        final Optional<SessionParticipants> participantsOptional =
                participantsRepository.findBySessionId(sessionId);

        if (participantsOptional.isPresent()) {
            final SessionParticipants participants = participantsOptional.get();
            if (!participants.isActive()) {
                throw new IllegalStateException("Η συνεδρία δεν είναι πλέον ενεργή.");
            }
            if (!participants.getPlayers().contains(cleanUsername)) {
                throw new SecurityException("Δεν συμμετέχεις σε αυτή τη συνεδρία.");
            }
            return participants.getPlayers();
        }

        if (!session.creatorUsername().equals(cleanUsername)) {
            throw new SecurityException("Δεν συμμετέχεις σε αυτή τη συνεδρία.");
        }
        return Set.of(session.creatorUsername());
    }
}