package com.escapehub.gameplay.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.escapehub.gameplay.domain.MissionRun;
import com.escapehub.gameplay.domain.MissionRunStatus;
import com.escapehub.gameplay.infrastructure.InMemoryMissionRunRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MissionRunServiceTest {

    private MutableClock clock;
    private MissionRunService service;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-11-19T10:00:00Z"));
        service = new MissionRunService(new InMemoryMissionRunRepository(), clock);
    }

    @Test
    void newPlayerHasNoRuns() {
        final MissionRunOverview overview = service.overviewFor("marios");

        assertTrue(overview.active().isEmpty());
        assertTrue(overview.completed().isEmpty());
    }

    @Test
    void startedRunAppearsAsActive() {
        final MissionRun run = service.startRun("marios", "Εργαστήριο");

        assertEquals(MissionRunStatus.ACTIVE, run.status());
        assertEquals(List.of(run), service.overviewFor("marios").active());
        assertTrue(service.overviewFor("marios").completed().isEmpty());
    }

    @Test
    void completedRunMovesToCompletedCategory() {
        final MissionRun run = service.startRun("marios", "Εργαστήριο");
        clock.advanceMinutes(30);

        final MissionRun done = service.completeRun(run.id(), "marios");

        assertEquals(MissionRunStatus.COMPLETED, done.status());
        assertTrue(service.overviewFor("marios").active().isEmpty());
        assertEquals(List.of(done), service.overviewFor("marios").completed());
        assertEquals(Instant.parse("2026-11-19T10:30:00Z"), done.completedAt());
    }

    @Test
    void runsAreSplitIntoCorrectCategoriesNewestFirst() {
        final MissionRun first = service.startRun("marios", "Πρώτη");
        clock.advanceMinutes(10);
        final MissionRun second = service.startRun("marios", "Δεύτερη");
        clock.advanceMinutes(10);
        final MissionRun third = service.startRun("marios", "Τρίτη");
        clock.advanceMinutes(10);
        final MissionRun firstDone = service.completeRun(first.id(), "marios");
        clock.advanceMinutes(10);
        final MissionRun thirdDone = service.completeRun(third.id(), "marios");

        final MissionRunOverview overview = service.overviewFor("marios");

        assertEquals(List.of(second), overview.active());
        assertEquals(List.of(thirdDone, firstDone), overview.completed());
    }

    @Test
    void playersOnlySeeTheirOwnRunsIgnoringCase() {
        service.startRun("marios", "Δική μου");
        service.startRun("other", "Ξένη");

        final MissionRunOverview mine = service.overviewFor("MARIOS");

        assertEquals(1, mine.active().size());
        assertEquals("Δική μου", mine.active().get(0).missionTitle());
    }

    @Test
    void onlyTheOwnerCanCompleteARun() {
        final MissionRun run = service.startRun("marios", "Εργαστήριο");

        assertThrows(NoSuchElementException.class, () -> service.completeRun(run.id(), "intruder"));
        assertEquals(1, service.overviewFor("marios").active().size());
    }

    @Test
    void unknownRunCannotBeCompleted() {
        assertThrows(NoSuchElementException.class, () -> service.completeRun(999, "marios"));
    }

    @Test
    void completedRunCannotBeCompletedTwice() {
        final MissionRun run = service.startRun("marios", "Εργαστήριο");
        service.completeRun(run.id(), "marios");

        assertThrows(IllegalStateException.class, () -> service.completeRun(run.id(), "marios"));
    }

    /** Ρολόι με ελεγχόμενη ώρα για ντετερμινιστικά tests. */
    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(final Instant start) {
            this.now = start;
        }

        void advanceMinutes(final long minutes) {
            now = now.plusSeconds(minutes * 60);
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(final java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
