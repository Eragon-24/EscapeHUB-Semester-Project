package com.escapehub.gameplay.domain;

import java.time.Instant;

/**
 * US-015: μία εκτέλεση (playthrough) μιας αποστολής από έναν παίκτη, με σαφή κατάσταση.
 *
 * <p>Είναι αμετάβλητο αντικείμενο: οι αλλαγές κατάστασης επιστρέφουν νέο {@code MissionRun}.
 * Μια εκτέλεση ξεκινά ως {@link MissionRunStatus#ACTIVE} και μπορεί να γίνει
 * {@link MissionRunStatus#COMPLETED} μία φορά.
 *
 * @param id μοναδικό αναγνωριστικό
 * @param ownerUsername ο παίκτης στον οποίο ανήκει η εκτέλεση
 * @param missionTitle ο τίτλος της αποστολής
 * @param status η τρέχουσα κατάσταση
 * @param startedAt πότε ξεκίνησε
 * @param completedAt πότε ολοκληρώθηκε (null όσο είναι ενεργή)
 */
public record MissionRun(
        long id,
        String ownerUsername,
        String missionTitle,
        MissionRunStatus status,
        Instant startedAt,
        Instant completedAt) {

    /**
     * Ελέγχει τη συνέπεια των πεδίων.
     */
    public MissionRun {
        if (ownerUsername == null || ownerUsername.isBlank()) {
            throw new IllegalArgumentException("Owner must not be blank");
        }
        if (missionTitle == null || missionTitle.isBlank()) {
            throw new IllegalArgumentException("Mission title must not be blank");
        }
        if (status == null || startedAt == null) {
            throw new IllegalArgumentException("Status and start time are required");
        }
        if (status == MissionRunStatus.COMPLETED && completedAt == null) {
            throw new IllegalArgumentException("A completed run needs a completion time");
        }
        if (status == MissionRunStatus.ACTIVE && completedAt != null) {
            throw new IllegalArgumentException("An active run cannot have a completion time");
        }
    }

    /**
     * Δημιουργεί νέα ενεργή εκτέλεση.
     *
     * @param id το αναγνωριστικό
     * @param ownerUsername ο παίκτης
     * @param missionTitle ο τίτλος της αποστολής
     * @param startedAt η στιγμή έναρξης
     * @return η ενεργή εκτέλεση
     */
    public static MissionRun start(
            final long id, final String ownerUsername, final String missionTitle, final Instant startedAt) {
        return new MissionRun(id, ownerUsername, missionTitle, MissionRunStatus.ACTIVE, startedAt, null);
    }

    /**
     * Ολοκληρώνει την εκτέλεση.
     *
     * @param when η στιγμή ολοκλήρωσης
     * @return η ολοκληρωμένη εκτέλεση
     * @throws IllegalStateException αν η εκτέλεση δεν είναι ενεργή
     */
    public MissionRun complete(final Instant when) {
        if (status != MissionRunStatus.ACTIVE) {
            throw new IllegalStateException("Only an active run can be completed");
        }
        return new MissionRun(id, ownerUsername, missionTitle, MissionRunStatus.COMPLETED, startedAt, when);
    }

    /**
     * Δείχνει αν η εκτέλεση είναι ενεργή.
     *
     * @return true αν είναι ενεργή
     */
    public boolean isActive() {
        return status == MissionRunStatus.ACTIVE;
    }
}
