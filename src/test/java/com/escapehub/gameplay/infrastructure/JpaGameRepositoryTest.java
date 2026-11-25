package com.escapehub.gameplay.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.escapehub.gameplay.application.GameRepository;
import com.escapehub.gameplay.domain.Game;
import com.escapehub.gameplay.domain.ItemType;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class JpaGameRepositoryTest {

    @Autowired
    private GameRepository games;

    @Test
    void persistsAndReloadsExitUnlockAndSharedProgress() {
        final UUID sessionId = UUID.randomUUID();
        final Game game = new Game(sessionId);
        game.putAttribute("room.clue", "found");
        game.completePuzzle("chest");
        game.collectItem(ItemType.LAB_KEY);
        game.unlockExit();

        games.save(game);

        final Game restored = games.findBySessionId(sessionId).orElseThrow();

        assertEquals(game.snapshot(), restored.snapshot());
        assertTrue(restored.snapshot().exitUnlocked());
        assertEquals(1, restored.snapshot().completedPuzzleIds().size());
        assertEquals(1, restored.snapshot().collectedItems().size());
    }

    @Test
    void updatesAnExistingDatabaseRecord() {
        final UUID sessionId = UUID.randomUUID();
        final Game game = new Game(sessionId);
        games.save(game);
        game.putAttribute("door.note", "updated");
        game.completePuzzle("chest");

        games.save(game);

        final Game restored = games.findBySessionId(sessionId).orElseThrow();
        assertEquals("updated", restored.snapshot().attributes().get("door.note"));
        assertEquals(java.util.Set.of("chest"), restored.snapshot().completedPuzzleIds());
    }
}
