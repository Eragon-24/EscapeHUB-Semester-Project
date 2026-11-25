package com.escapehub.gameplay.infrastructure;

import com.escapehub.gameplay.application.GameRepository;
import com.escapehub.gameplay.domain.Game;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryGameRepository implements GameRepository {

    private final Map<UUID, Game> games = new HashMap<>();

    @Override
    public synchronized Game save(final Game game) {
        if (game == null) {
            throw new IllegalArgumentException("Game must not be null");
        }
        games.put(game.getSessionId(), game);
        return game;
    }

    @Override
    public synchronized Optional<Game> findBySessionId(final UUID sessionId) {
        return Optional.ofNullable(games.get(sessionId));
    }
}