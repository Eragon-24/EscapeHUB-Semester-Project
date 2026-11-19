package com.escapehub.gameplay.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class MissionRunTest {

    private static final Instant START = Instant.parse("2026-11-19T10:00:00Z");
    private static final Instant END = Instant.parse("2026-11-19T11:00:00Z");

    @Test
    void newRunIsActiveWithoutCompletionTime() {
        final MissionRun run = MissionRun.start(1, "marios", "Εργαστήριο", START);

        assertEquals(MissionRunStatus.ACTIVE, run.status());
        assertTrue(run.isActive());
        assertNull(run.completedAt());
    }

    @Test
    void completingActiveRunMakesItCompleted() {
        final MissionRun done = MissionRun.start(1, "marios", "Εργαστήριο", START).complete(END);

        assertEquals(MissionRunStatus.COMPLETED, done.status());
        assertFalse(done.isActive());
        assertEquals(END, done.completedAt());
        assertEquals(START, done.startedAt());
    }

    @Test
    void completedRunCannotBeCompletedAgain() {
        final MissionRun done = MissionRun.start(1, "marios", "Εργαστήριο", START).complete(END);

        assertThrows(IllegalStateException.class, () -> done.complete(END));
    }

    @Test
    void invalidFieldsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> MissionRun.start(1, " ", "Τίτλος", START));
        assertThrows(IllegalArgumentException.class, () -> MissionRun.start(1, "marios", "", START));
        assertThrows(IllegalArgumentException.class, () -> MissionRun.start(1, "marios", "Τίτλος", null));
        assertThrows(IllegalArgumentException.class, () ->
                new MissionRun(1, "marios", "Τίτλος", MissionRunStatus.COMPLETED, START, null));
        assertThrows(IllegalArgumentException.class, () ->
                new MissionRun(1, "marios", "Τίτλος", MissionRunStatus.ACTIVE, START, END));
    }

    @Test
    void statusesHaveGreekLabels() {
        assertEquals("Ενεργή", MissionRunStatus.ACTIVE.getLabel());
        assertEquals("Ολοκληρωμένη", MissionRunStatus.COMPLETED.getLabel());
    }
}
