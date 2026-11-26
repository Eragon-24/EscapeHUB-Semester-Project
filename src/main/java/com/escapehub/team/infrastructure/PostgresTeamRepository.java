package com.escapehub.team.infrastructure;

import com.escapehub.team.domain.DuplicateJoinCodeException;
import com.escapehub.team.domain.Team;
import com.escapehub.team.domain.TeamRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
@Profile("postgres")
public class PostgresTeamRepository implements TeamRepository {

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public PostgresTeamRepository(
            final JdbcTemplate jdbcTemplate,
            final PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public Team save(final Team team) {
        try {
            Long savedId = transactionTemplate.execute(status -> {
                Long teamId;

                if (team.getId() == null) {
                    teamId = jdbcTemplate.queryForObject(
                            """
                            INSERT INTO team (name, join_code)
                            VALUES (?, ?)
                            RETURNING id
                            """,
                            Long.class,
                            team.getName(),
                            team.getJoinCode()
                    );
                } else {
                    teamId = team.getId();

                    int updated = jdbcTemplate.update(
                            """
                            UPDATE team
                            SET name = ?, join_code = ?
                            WHERE id = ?
                            """,
                            team.getName(),
                            team.getJoinCode(),
                            teamId
                    );

                    if (updated == 0) {
                        throw new IllegalStateException("Team not found: " + teamId);
                    }

                    jdbcTemplate.update(
                            "DELETE FROM team_member WHERE team_id = ?",
                            teamId
                    );
                }

                for (Team.Member member : team.getMembers()) {
                    jdbcTemplate.update(
                            """
                            INSERT INTO team_member (team_id, user_id, role)
                            VALUES (?, ?, ?)
                            """,
                            teamId,
                            member.userId(),
                            member.role().name()
                    );
                }

                return teamId;
            });

            if (team.getId() == null) {
                team.setId(savedId);
            }

            return team;
        } catch (DuplicateKeyException exception) {
            if (existsByJoinCode(team.getJoinCode())) {
                throw new DuplicateJoinCodeException(team.getJoinCode());
            }
            throw exception;
        }
    }

    @Override
    public boolean existsByJoinCode(final String joinCode) {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM team WHERE join_code = ?)",
                Boolean.class,
                joinCode
        );
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public Optional<Team> findByJoinCode(final String joinCode) {
        List<Team> teams = jdbcTemplate.query(
                """
                SELECT id, name, join_code
                FROM team
                WHERE join_code = ?
                """,
                (resultSet, rowNumber) -> Team.restore(
                        resultSet.getLong("id"),
                        resultSet.getString("name"),
                        resultSet.getString("join_code"),
                        List.of()
                ),
                joinCode
        );

        if (teams.isEmpty()) {
            return Optional.empty();
        }

        Team team = teams.get(0);

        List<Team.Member> members = jdbcTemplate.query(
                """
                SELECT user_id, role
                FROM team_member
                WHERE team_id = ?
                ORDER BY user_id
                """,
                (resultSet, rowNumber) -> new Team.Member(
                        resultSet.getLong("user_id"),
                        Team.Role.valueOf(resultSet.getString("role"))
                ),
                team.getId()
        );

        return Optional.of(
                Team.restore(team.getId(), team.getName(), team.getJoinCode(), members)
        );
    }
}
