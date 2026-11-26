package com.escapehub.gameplay.infrastructure;

import com.escapehub.gameplay.application.GameRepository;
import com.escapehub.gameplay.domain.Game;
import com.escapehub.gameplay.domain.GameState;
import com.escapehub.gameplay.domain.GameStatus;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
@Profile("postgres")
public class PostgresGameRepository implements GameRepository {

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public PostgresGameRepository(
            final JdbcTemplate jdbcTemplate,
            final PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public Game save(final Game game) {
        if (game == null) {
            throw new IllegalArgumentException("Game must not be null");
        }

        transactionTemplate.executeWithoutResult(status -> {
            GameState snapshot = game.snapshot();

            final int updatedState = jdbcTemplate.update(
                    "UPDATE game_state SET status = ?, version = ? WHERE session_id = ?",
                    snapshot.status().name(), snapshot.version(), snapshot.sessionId());
            if (updatedState == 0) {
                jdbcTemplate.update(
                        "INSERT INTO game_state (session_id, status, version) VALUES (?, ?, ?)",
                        snapshot.sessionId(), snapshot.status().name(), snapshot.version());
            }

            jdbcTemplate.update(
                    "DELETE FROM game_attribute WHERE session_id = ?",
                    snapshot.sessionId()
            );

            for (Map.Entry<String, String> entry : snapshot.attributes().entrySet()) {
                jdbcTemplate.update(
                        """
                                INSERT INTO game_attribute
                                    (session_id, attribute_key, attribute_value)
                                VALUES (?, ?, ?)
                                """,
                        snapshot.sessionId(),
                        entry.getKey(),
                        entry.getValue()
                );
            }

            final int updatedRoom = jdbcTemplate.update(
                    "UPDATE game_attribute SET attribute_value = ? WHERE session_id = ? AND attribute_key = ?",
                    snapshot.currentRoomId(), snapshot.sessionId(), "game.currentRoomId");
            if (updatedRoom == 0) {
                jdbcTemplate.update(
                        "INSERT INTO game_attribute (session_id, attribute_key, attribute_value) VALUES (?, ?, ?)",
                        snapshot.sessionId(), "game.currentRoomId", snapshot.currentRoomId());
            }
        });

        return game;
    }

    @Override
    public Optional<Game> findBySessionId(final UUID sessionId) {
        List<GameState> states = jdbcTemplate.query(
                """
                        SELECT status, version
                        FROM game_state
                        WHERE session_id = ?
                        """,
                (rs, row) -> {
                    String status = rs.getString("status");
                    return new GameState(
                            sessionId,
                            Game.INITIAL_ROOM_ID,
                            GameStatus.valueOf(status),
                            Map.of(),
                            rs.getLong("version")
                    );
                },
                sessionId
        );

        if (states.isEmpty()) {
            return Optional.empty();
        }

        Map<String, String> attributes = new java.util.HashMap<>();

        List<Map<String, Object>> attributeRows = jdbcTemplate.queryForList(
                """
                        SELECT attribute_key, attribute_value
                        FROM game_attribute
                        WHERE session_id = ?
                        """,
                sessionId
        );

        for (Map<String, Object> row : attributeRows) {
            attributes.put(
                    (String) row.get("attribute_key"),
                    (String) row.get("attribute_value")
            );
        }

        GameState state = states.get(0);

        String roomId = attributes.getOrDefault(
                "game.currentRoomId",
                Game.INITIAL_ROOM_ID
        );

        attributes.remove("game.currentRoomId");

        return Optional.of(Game.restore(
                sessionId,
                roomId,
                state.status(),
                attributes,
                state.version()
        ));
    }
}
