package com.escapehub.user.infrastructure;

import com.escapehub.user.application.DuplicateUserException;
import com.escapehub.user.application.UserRepository;
import com.escapehub.user.domain.User;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Profile("postgres")
public class PostgresUserRepository implements UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public PostgresUserRepository(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public User save(final String username, final String email, final String passwordHash) {
        try {
            Long id = jdbcTemplate.queryForObject(
                    """
                    INSERT INTO user_account (username, email, password_hash)
                    VALUES (?, ?, ?)
                    RETURNING id
                    """,
                    Long.class,
                    username,
                    email,
                    passwordHash
            );

            return new User(id, username, email, passwordHash);
        } catch (DuplicateKeyException exception) {
            if (existsByUsername(username)) {
                throw new DuplicateUserException("username");
            }
            if (existsByEmail(email)) {
                throw new DuplicateUserException("email");
            }
            throw exception;
        }
    }

    @Override
    public Optional<User> findByUsername(final String username) {
        List<User> users = jdbcTemplate.query(
                """
                SELECT id, username, email, password_hash
                FROM user_account
                WHERE LOWER(username) = LOWER(?)
                """,
                (resultSet, rowNumber) -> new User(
                        resultSet.getLong("id"),
                        resultSet.getString("username"),
                        resultSet.getString("email"),
                        resultSet.getString("password_hash")
                ),
                username
        );

        return users.stream().findFirst();
    }

    @Override
    public Optional<User> findByEmail(final String email) {
        List<User> users = jdbcTemplate.query(
                """
                SELECT id, username, email, password_hash
                FROM user_account
                WHERE LOWER(email) = LOWER(?)
                """,
                (resultSet, rowNumber) -> new User(
                        resultSet.getLong("id"),
                        resultSet.getString("username"),
                        resultSet.getString("email"),
                        resultSet.getString("password_hash")
                ),
                email
        );

        return users.stream().findFirst();
    }

    private boolean existsByUsername(final String username) {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM user_account WHERE LOWER(username) = LOWER(?))",
                Boolean.class,
                username
        );
        return Boolean.TRUE.equals(exists);
    }

    private boolean existsByEmail(final String email) {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM user_account WHERE LOWER(email) = LOWER(?))",
                Boolean.class,
                email
        );
        return Boolean.TRUE.equals(exists);
    }
}
