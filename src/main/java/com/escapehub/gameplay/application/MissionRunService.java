package com.escapehub.gameplay.application;

import com.escapehub.gameplay.domain.MissionRun;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;

/**
 * US-015: διαχείριση της κατάστασης των MissionRun ενός παίκτη.
 *
 * <p>Η δημιουργία ({@link #startRun}) είναι ελάχιστη και θα επεκταθεί από το US-014 (έναρξη MissionRun),
 * η ολοκλήρωση ({@link #completeRun}) από το US-040.
 */
@Service
public class MissionRunService {

    private final MissionRunRepository runs;
    private final Clock clock;

    /**
     * Δημιουργεί την υπηρεσία.
     *
     * @param runs το repository εκτελέσεων
     * @param clock το ρολόι (για ντετερμινιστικά tests)
     */
    public MissionRunService(final MissionRunRepository runs, final Clock clock) {
        this.runs = runs;
        this.clock = clock;
    }

    /**
     * Ξεκινά νέα ενεργή εκτέλεση για τον παίκτη.
     *
     * @param ownerUsername ο παίκτης
     * @param missionTitle ο τίτλος της αποστολής
     * @return η νέα ενεργή εκτέλεση
     */
    public MissionRun startRun(final String ownerUsername, final String missionTitle) {
        return runs.save(MissionRun.start(runs.nextId(), ownerUsername, missionTitle, Instant.now(clock)));
    }

    /**
     * Ολοκληρώνει μια ενεργή εκτέλεση. Μόνο ο ιδιοκτήτης μπορεί να την ολοκληρώσει.
     *
     * @param runId το id της εκτέλεσης
     * @param ownerUsername ο παίκτης που ζητά την ολοκλήρωση
     * @return η ολοκληρωμένη εκτέλεση
     * @throws NoSuchElementException αν δεν υπάρχει εκτέλεση με αυτό το id για τον παίκτη
     * @throws IllegalStateException αν η εκτέλεση δεν είναι ενεργή
     */
    public MissionRun completeRun(final long runId, final String ownerUsername) {
        final MissionRun run = runs.findById(runId)
                .filter(r -> r.ownerUsername().equalsIgnoreCase(ownerUsername))
                .orElseThrow(() -> new NoSuchElementException("No such mission run"));
        return runs.save(run.complete(Instant.now(clock)));
    }

    /**
     * Επιστρέφει τις εκτελέσεις του παίκτη ομαδοποιημένες σε ενεργές και ολοκληρωμένες.
     *
     * @param ownerUsername ο παίκτης
     * @return η επισκόπηση
     */
    public MissionRunOverview overviewFor(final String ownerUsername) {
        final List<MissionRun> all = runs.findByOwner(ownerUsername);
        final List<MissionRun> active = all.stream()
                .filter(MissionRun::isActive)
                .sorted(Comparator.comparing(MissionRun::startedAt).reversed()
                        .thenComparing(Comparator.comparingLong(MissionRun::id).reversed()))
                .toList();
        final List<MissionRun> completed = all.stream()
                .filter(r -> !r.isActive())
                .sorted(Comparator.comparing(MissionRun::completedAt).reversed()
                        .thenComparing(Comparator.comparingLong(MissionRun::id).reversed()))
                .toList();
        return new MissionRunOverview(active, completed);
    }
}
