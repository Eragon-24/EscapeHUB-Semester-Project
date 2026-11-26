package com.escapehub.gameplay.infrastructure;

import com.escapehub.gameplay.application.GameRepository;
import com.escapehub.gameplay.domain.Game;
import com.escapehub.gameplay.infrastructure.jpa.GameStateEntity;
import com.escapehub.gameplay.infrastructure.jpa.SpringDataGameStateRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * PostgreSQL/H2 adapter for authoritative shared game state.
 */
@Repository
@Primary
public class JpaGameRepository implements GameRepository {

    private final SpringDataGameStateRepository states;

    public JpaGameRepository(final SpringDataGameStateRepository states) {
        this.states = states;
    }

    @Override
    @Transactional
    public Game save(final Game game) {
        if (game == null) {
            throw new IllegalArgumentException("Game must not be null");
        }
        final GameStateEntity entity = states.findById(game.getSessionId())
                .orElseGet(() -> new GameStateEntity(game.snapshot()));
        entity.updateFrom(game.snapshot());
        states.save(entity);
        return game;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Game> findBySessionId(final UUID sessionId) {
        return states.findById(sessionId).map(entity -> Game.restore(entity.toDomain()));
    }
}
