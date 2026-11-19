package com.escapehub.gameplay.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.escapehub.gameplay.domain.MissionRun;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class InMemoryMissionRunRepositoryTest {

    private static final Instant START = Instant.parse("2026-11-19T10:00:00Z");

    private final InMemoryMissionRunRepository repository = new InMemoryMissionRunRepository();

    @Test
    void idsIncrease() {
        assertEquals(1, repository.nextId());
        assertEquals(2, repository.nextId());
    }

    @Test
    void savedRunCanBeFoundAndReplaced() {
        final MissionRun run = repository.save(MissionRun.start(repository.nextId(), "marios", "Α", START));

        assertEquals(run, repository.findById(run.id()).orElseThrow());

        final MissionRun done = repository.save(run.complete(START.plusSeconds(60)));

        assertEquals(done, repository.findById(run.id()).orElseThrow());
        assertEquals(1, repository.findByOwner("marios").size());
    }

    @Test
    void findByOwnerIgnoresCaseAndOtherPlayers() {
        repository.save(MissionRun.start(repository.nextId(), "Marios", "Α", START));
        repository.save(MissionRun.start(repository.nextId(), "other", "Β", START));

        assertEquals(1, repository.findByOwner("marios").size());
        assertTrue(repository.findByOwner("nobody").isEmpty());
        assertTrue(repository.findById(42).isEmpty());
    }
}
