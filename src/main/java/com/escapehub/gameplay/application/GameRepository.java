package com.escapehub.gameplay.application;

import com.escapehub.gameplay.domain.Game;
import java.util.Optional;
import java.util.UUID;

public interface GameRepository {
    Game save(Game game);
    Optional<Game> findBySessionId(UUID sessionId);
}