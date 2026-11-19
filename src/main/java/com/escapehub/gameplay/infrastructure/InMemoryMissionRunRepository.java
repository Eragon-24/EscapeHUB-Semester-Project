package com.escapehub.gameplay.infrastructure;

import com.escapehub.gameplay.application.MissionRunRepository;
import com.escapehub.gameplay.domain.MissionRun;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * US-015: προσωρινή αποθήκευση των MissionRun στη μνήμη. Θα αντικατασταθεί από βάση δεδομένων (persistence, W8).
 */
@Repository
public class InMemoryMissionRunRepository implements MissionRunRepository {

    private final Map<Long, MissionRun> runs = new HashMap<>();
    private long lastId;

    @Override
    public synchronized long nextId() {
        return ++lastId;
    }

    @Override
    public synchronized MissionRun save(final MissionRun run) {
        runs.put(run.id(), run);
        return run;
    }

    @Override
    public synchronized Optional<MissionRun> findById(final long id) {
        return Optional.ofNullable(runs.get(id));
    }

    @Override
    public synchronized List<MissionRun> findByOwner(final String ownerUsername) {
        return runs.values().stream()
                .filter(r -> r.ownerUsername().equalsIgnoreCase(ownerUsername))
                .toList();
    }
}
