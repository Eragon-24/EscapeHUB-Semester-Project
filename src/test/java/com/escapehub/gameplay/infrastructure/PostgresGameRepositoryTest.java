package com.escapehub.gameplay.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.escapehub.gameplay.domain.Game;
import com.escapehub.gameplay.domain.GameStatus;
import java.util.Map;
import java.util.UUID;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

class PostgresGameRepositoryTest {

    private PostgresGameRepository repository;

    @BeforeEach
    void setUp() {
        final JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:postgres-game-repository;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        final JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS game_state (session_id UUID PRIMARY KEY, status VARCHAR(32), version BIGINT)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS game_attribute (session_id UUID, attribute_key VARCHAR(128), "
                + "attribute_value VARCHAR(1000), PRIMARY KEY(session_id, attribute_key))");
        repository = new PostgresGameRepository(
                jdbcTemplate, new DataSourceTransactionManager(dataSource));
    }

    @Test
    void savesAndReloadsSessionInventoryAttributes() {
        final UUID sessionId = UUID.randomUUID();
        final Game game = Game.restore(sessionId, Game.INITIAL_ROOM_ID, GameStatus.IN_PROGRESS,
                Map.of("inventory.lab-notebook", "true", "inventory.silver-key", "true"), 2);

        repository.save(game);
        final Game restored = repository.findBySessionId(sessionId).orElseThrow();

        assertEquals("true", restored.snapshot().attributes().get("inventory.lab-notebook"));
        assertEquals("true", restored.snapshot().attributes().get("inventory.silver-key"));
        assertEquals(2, restored.snapshot().version());
    }

    @Test
    void saveUpdatesExistingInventoryValues() {
        final UUID sessionId = UUID.randomUUID();
        final Game game = new Game(sessionId);
        repository.save(game);
        game.putAttribute("inventory.lab-notebook", "true");

        repository.save(game);

        assertTrue(repository.findBySessionId(sessionId).orElseThrow().snapshot()
                .attributes().containsKey("inventory.lab-notebook"));
import com.escapehub.gameplay.application.GameRepository;
import com.escapehub.gameplay.domain.Game;
import com.escapehub.gameplay.domain.ItemType;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * US-024: the puzzle progress and the exit state survive a round trip through PostgreSQL (migration V4).
 * Runs only when a database is configured through {@code DB_URL}, as in CI.
 */
@SpringBootTest
@ActiveProfiles("postgres")
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class PostgresGameRepositoryTest {

    @Autowired
    private GameRepository games;

    @Autowired
    private JdbcTemplate jdbc;

    private final String suffix = UUID.randomUUID().toString().substring(0, 6);
    private UUID sessionId;

    @BeforeEach
    void createSession() {
        final Long userId = jdbc.queryForObject(
                "INSERT INTO user_account (username, email, password_hash) VALUES (?, ?, 'x') RETURNING id",
                Long.class, "game_" + suffix, "game_" + suffix + "@example.com");
        sessionId = UUID.randomUUID();
        jdbc.update("INSERT INTO game_session (id, join_code, creator_user_id) VALUES (?, ?, ?)",
                sessionId, suffix.toUpperCase(Locale.ROOT), userId);
    }

    @AfterEach
    void removeSession() {
        jdbc.update("DELETE FROM game_session WHERE id = ?", sessionId);
        jdbc.update("DELETE FROM user_account WHERE username = ?", "game_" + suffix);
    }

    @Test
    void progressItemsAndExitStateAreReloaded() {
        final Game game = new Game(sessionId);
        game.putAttribute("room.clue", "found");
        game.completePuzzle("chest");
        game.collectItem(ItemType.LAB_KEY);
        game.unlockExit();

        games.save(game);

        assertEquals(game.snapshot(), games.findBySessionId(sessionId).orElseThrow().snapshot());
    }

    @Test
    void savingAgainUpdatesTheStoredProgress() {
        final Game game = new Game(sessionId);
        games.save(game);
        game.completePuzzle("chest");
        game.unlockExit();

        games.save(game);
        games.save(game);

        final Game restored = games.findBySessionId(sessionId).orElseThrow();
        assertTrue(restored.snapshot().exitUnlocked());
        assertEquals(game.snapshot(), restored.snapshot());
    }
}
