package com.escapehub.gameplay.infrastructure;

import com.escapehub.gameplay.application.GameSessionRepository;
import com.escapehub.gameplay.domain.GameSession;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Profile("postgres")
public class PostgresGameSessionRepository implements GameSessionRepository {

    private final JdbcTemplate jdbcTemplate;

    public PostgresGameSessionRepository(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public GameSession save(final GameSession session) {
        if (session == null) {
            throw new IllegalArgumentException("Session must not be null");
        }

        try {
            Long creatorUserId = jdbcTemplate.queryForObject(
                    """
                    SELECT id
                    FROM user_account
                    WHERE LOWER(username) = LOWER(?)
                    """,
                    Long.class,
                    session.creatorUsername()
            );

            jdbcTemplate.update(
                    """
                    INSERT INTO game_session (id, join_code, creator_user_id, created_at)
                    VALUES (?, ?, ?, ?)
                    """,
                    session.id(),
                    session.joinCode(),
                    creatorUserId,
                    Timestamp.from(session.createdAt())
            );

            return session;
        } catch (DuplicateKeyException exception) {
            if (existsByJoinCode(session.joinCode())) {
                throw new IllegalArgumentException("Join code is already in use", exception);
            }
            throw exception;
        }
    }

    @Override
    public boolean existsByJoinCode(final String joinCode) {
        Boolean exists = jdbcTemplate.queryForObject(
                """
                SELECT EXISTS (
                    SELECT 1 FROM game_session WHERE join_code = ?
                )
                """,
                Boolean.class,
                joinCode
        );

        return Boolean.TRUE.equals(exists);
    }

    @Override
    public Optional<GameSession> findByJoinCode(final String joinCode) {
        return findSession(
                """
                WHERE gs.join_code = ?
                """,
                joinCode
        );
    }

    @Override
    public Optional<GameSession> findById(final UUID id) {
        return findSession(
                """
                WHERE gs.id = ?
                """,
                id
        );
    }

    private Optional<GameSession> findSession(
            final String condition,
            final Object parameter) {

        List<GameSession> sessions = jdbcTemplate.query(
                """
                SELECT gs.id, gs.join_code, ua.username AS creator_username, gs.created_at
                FROM game_session gs
                JOIN user_account ua ON ua.id = gs.creator_user_id
                """ + condition,
                (resultSet, rowNumber) -> new GameSession(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("join_code"),
                        resultSet.getString("creator_username"),
                        resultSet.getTimestamp("created_at").toInstant()
                ),
                parameter
        );

        return sessions.stream().findFirst();
    }
}
