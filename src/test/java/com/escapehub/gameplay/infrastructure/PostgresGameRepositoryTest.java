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
    }
}
