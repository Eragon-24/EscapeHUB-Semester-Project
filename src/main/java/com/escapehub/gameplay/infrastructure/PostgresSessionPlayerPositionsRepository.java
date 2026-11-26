
package com.escapehub.gameplay.infrastructure;

import com.escapehub.gameplay.application.SessionPlayerPositionsRepository;
import com.escapehub.gameplay.domain.Game;
import com.escapehub.gameplay.domain.movement.PlayerPosition;
import com.escapehub.gameplay.domain.movement.SessionPlayerPositions;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
@Profile("postgres")
public class PostgresSessionPlayerPositionsRepository
        implements SessionPlayerPositionsRepository {

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public PostgresSessionPlayerPositionsRepository(
            final JdbcTemplate jdbcTemplate,
            final PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public SessionPlayerPositions save(final SessionPlayerPositions positions) {
        if (positions == null) {
            throw new IllegalArgumentException("Session positions must not be null");
        }

        transactionTemplate.executeWithoutResult(status -> {
            for (var entry : positions.getPositions().entrySet()) {
                Long userId = jdbcTemplate.queryForObject(
                        """
                        SELECT id
                        FROM user_account
                        WHERE LOWER(username) = LOWER(?)
                        """,
                        Long.class,
                        entry.getKey()
                );

                PlayerPosition position = entry.getValue();

                jdbcTemplate.update(
                        """
                        INSERT INTO session_player_positions
                            (session_id, user_id, room_id, position_x,
                             position_y, visible, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                        ON CONFLICT (session_id, user_id)
                        DO UPDATE SET room_id = EXCLUDED.room_id,
                                      position_x = EXCLUDED.position_x,
                                      position_y = EXCLUDED.position_y,
                                      visible = EXCLUDED.visible,
                                      updated_at = CURRENT_TIMESTAMP
                        """,
                        positions.getSessionId(),
                        userId,
                        Game.INITIAL_ROOM_ID,
                        position.x(),
                        position.y(),
                        positions.isVisible(entry.getKey())
                );
            }
        });

        return positions;
    }

    @Override
    public Optional<SessionPlayerPositions> findBySessionId(final UUID sessionId) {
        List<PositionRow> rows = jdbcTemplate.query(
                """
                SELECT ua.username, spp.position_x, spp.position_y, spp.visible
                FROM session_player_positions spp
                JOIN user_account ua ON ua.id = spp.user_id
                WHERE spp.session_id = ?
                ORDER BY ua.username
                """,
                (rs, row) -> new PositionRow(
                        rs.getString("username"),
                        new PlayerPosition(
                                rs.getDouble("position_x"),
                                rs.getDouble("position_y")),
                        rs.getBoolean("visible")
                ),
                sessionId
        );

        if (rows.isEmpty()) {
            return Optional.empty();
        }

        SessionPlayerPositions positions = new SessionPlayerPositions(sessionId);

        for (PositionRow row : rows) {
            positions.updatePosition(row.username(), row.position());

            if (!row.visible()) {
                positions.removePlayer(row.username());
            }
        }

        return Optional.of(positions);
    }

    private record PositionRow(
            String username,
            PlayerPosition position,
            boolean visible) {
    }
}
