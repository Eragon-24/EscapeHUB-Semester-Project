package com.escapehub.evidence.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.escapehub.evidence.application.EvidenceRepository;
import com.escapehub.evidence.domain.EvidenceEntry;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("postgres")
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class CollectedEvidenceMigrationPostgresTest {

    @Autowired
    private EvidenceRepository repository;

    @Autowired
    private JdbcTemplate jdbc;

    private UUID createSession(final String suffix) {
        final Long userId = jdbc.queryForObject(
                "INSERT INTO user_account (username, email, password_hash) "
                        + "VALUES (?, ?, 'x') RETURNING id",
                Long.class, "mig_" + suffix, "mig_" + suffix + "@example.com");
        final UUID sessionId = UUID.randomUUID();
        jdbc.update("INSERT INTO game_session (id, join_code, creator_user_id) VALUES (?, ?, ?)",
                sessionId, suffix.toUpperCase(java.util.Locale.ROOT), userId);
        return sessionId;
    }

    @Test
    void migrationV3IsAppliedAndEvidenceRoundTripsThroughPostgres() {
        final String suffix = UUID.randomUUID().toString().substring(0, 6).replace("-", "a");
        final UUID sessionId = createSession(suffix);
        try {
            final Integer applied = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM flyway_schema_history "
                            + "WHERE version = '3' AND success = TRUE", Integer.class);
            assertEquals(1, applied);

            final Instant now = Instant.parse("2026-11-26T10:00:00Z");
            final EvidenceEntry first = new EvidenceEntry(
                    UUID.randomUUID(), sessionId, "Σημειωματάριο", "Κωδικός", now);
            final EvidenceEntry second = new EvidenceEntry(
                    UUID.randomUUID(), sessionId, "Σεντούκι", "Κλειδωμένο", now.plusSeconds(1));
            repository.save(second);
            repository.save(first);

            assertEquals(List.of(first, second), repository.findBySessionId(sessionId));
        } finally {
            jdbc.update("DELETE FROM game_session WHERE id = ?", sessionId);
            jdbc.update("DELETE FROM user_account WHERE username = ?", "mig_" + suffix);
        }
    }

    @Test
    void evidenceForUnknownSessionIsRejectedByForeignKey() {
        final EvidenceEntry orphan = new EvidenceEntry(
                UUID.randomUUID(), UUID.randomUUID(), "Orphan", "no session", Instant.now());

        assertThrows(DataIntegrityViolationException.class, () -> repository.save(orphan));
    }
}