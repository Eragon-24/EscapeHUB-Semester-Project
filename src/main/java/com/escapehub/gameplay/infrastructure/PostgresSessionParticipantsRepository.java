package com.escapehub.gameplay.infrastructure;

import com.escapehub.gameplay.application.SessionParticipantsRepository;
import com.escapehub.gameplay.domain.SessionParticipants;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
@Profile("postgres")
public class PostgresSessionParticipantsRepository implements SessionParticipantsRepository {

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public PostgresSessionParticipantsRepository(
            final JdbcTemplate jdbcTemplate,
            final PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public SessionParticipants save(final SessionParticipants participants) {
        if (participants == null) {
            throw new IllegalArgumentException("Participants must not be null");
        }

        transactionTemplate.executeWithoutResult(status -> {
            UUID sessionId = participants.getSessionId();

            List<String> previouslyActive = jdbcTemplate.query(
                    """
                    SELECT ua.username
                    FROM session_participant sp
                    JOIN user_account ua ON ua.id = sp.user_id
                    WHERE sp.session_id = ? AND sp.left_at IS NULL
                    """,
                    (resultSet, rowNumber) -> resultSet.getString("username"),
                    sessionId
            );

            Set<String> currentUsernames = new HashSet<>();
            for (String username : participants.getPlayers()) {
                currentUsernames.add(username.toLowerCase(Locale.ROOT));
            }

            for (String username : participants.getPlayers()) {
                Long userId = jdbcTemplate.queryForObject(
                        """
                        SELECT id
                        FROM user_account
                        WHERE LOWER(username) = LOWER(?)
                        """,
                        Long.class,
                        username
                );

                Boolean wasActive = jdbcTemplate.queryForObject(
                        """
                        SELECT EXISTS (
                            SELECT 1
                            FROM session_participant
                            WHERE session_id = ?
                              AND user_id = ?
                              AND left_at IS NULL
                        )
                        """,
                        Boolean.class,
                        sessionId,
                        userId
                );

                jdbcTemplate.update(
                        """
                        INSERT INTO session_participant (session_id, user_id)
                        VALUES (?, ?)
                        ON CONFLICT (session_id, user_id)
                        DO UPDATE SET left_at = NULL
                        """,
                        sessionId,
                        userId
                );

                if (!Boolean.TRUE.equals(wasActive)) {
                    jdbcTemplate.update(
                            """
                            INSERT INTO participant_connection_history (session_id, user_id)
                            VALUES (?, ?)
                            """,
                            sessionId,
                            userId
                    );
                }
            }

            for (String username : previouslyActive) {
                if (!currentUsernames.contains(username.toLowerCase(Locale.ROOT))) {
                    jdbcTemplate.update(
                            """
                            UPDATE session_participant
                            SET left_at = CURRENT_TIMESTAMP
                            WHERE session_id = ?
                              AND user_id = (
                                  SELECT id FROM user_account
                                  WHERE LOWER(username) = LOWER(?)
                              )
                              AND left_at IS NULL
                            """,
                            sessionId,
                            username
                    );

                    jdbcTemplate.update(
                            """
                            UPDATE participant_connection_history
                            SET disconnected_at = CURRENT_TIMESTAMP
                            WHERE session_id = ?
                              AND user_id = (
                                  SELECT id FROM user_account
                                  WHERE LOWER(username) = LOWER(?)
                              )
                              AND disconnected_at IS NULL
                            """,
                            sessionId,
                            username
                    );
                }
            }
        });

        return participants;
    }

    @Override
    public Optional<SessionParticipants> findBySessionId(final UUID sessionId) {
        List<String> creators = jdbcTemplate.query(
                """
                SELECT ua.username
                FROM game_session gs
                JOIN user_account ua ON ua.id = gs.creator_user_id
                WHERE gs.id = ?
                """,
                (resultSet, rowNumber) -> resultSet.getString("username"),
                sessionId
        );

        if (creators.isEmpty()) {
            return Optional.empty();
        }

        String creatorUsername = creators.get(0);

        List<String> activePlayers = jdbcTemplate.query(
                """
                SELECT ua.username
                FROM session_participant sp
                JOIN user_account ua ON ua.id = sp.user_id
                WHERE sp.session_id = ? AND sp.left_at IS NULL
                ORDER BY sp.joined_at, sp.user_id
                """,
                (resultSet, rowNumber) -> resultSet.getString("username"),
                sessionId
        );

        Boolean hasParticipantRecords = jdbcTemplate.queryForObject(
                """
                SELECT EXISTS (
                    SELECT 1
                    FROM session_participant
                    WHERE session_id = ?
                )
                """,
                Boolean.class,
                sessionId
        );

        if (!Boolean.TRUE.equals(hasParticipantRecords)) {
            return Optional.empty();
        }

        SessionParticipants participants =
                new SessionParticipants(sessionId, creatorUsername);

        if (activePlayers.stream().noneMatch(
                username -> username.equalsIgnoreCase(creatorUsername))) {
            participants.removePlayer(creatorUsername);
        }

        for (String username : activePlayers) {
            participants.addPlayer(username);
        }

        return Optional.of(participants);
    }
}
