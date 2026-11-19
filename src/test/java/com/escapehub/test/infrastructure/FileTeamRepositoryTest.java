package com.escapehub.test.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.escapehub.team.domain.DuplicateJoinCodeException;
import com.escapehub.team.domain.Team;
import com.escapehub.team.infrastructure.FileTeamRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileTeamRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void savesTeamsWithIncreasingIdsAndWritesToFile() throws IOException {
        Path file = tempDir.resolve("data/teams.txt");
        FileTeamRepository repository = new FileTeamRepository(file.toString());

        Team first = repository.save(Team.create("Escapers", 42L, "AAAAAA"));
        Team second = repository.save(Team.create("Ομάδα Β", 7L, "BBBBBB"));

        assertEquals(1L, first.getId());
        assertEquals(2L, second.getId());
        List<String> lines = Files.readAllLines(file);
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).endsWith("|AAAAAA|42:CREATOR"));
    }

    @Test
    void rejectsDuplicateJoinCodeWhenSavingNewTeam() {
        FileTeamRepository repository =
                new FileTeamRepository(tempDir.resolve("teams.txt").toString());
        repository.save(Team.create("First", 1L, "AAAAAA"));

        assertThrows(DuplicateJoinCodeException.class,
                () -> repository.save(Team.create("Second", 2L, "AAAAAA")));
    }

    @Test
    void findsExistingJoinCodeOnly() {
        FileTeamRepository repository =
                new FileTeamRepository(tempDir.resolve("teams.txt").toString());
        assertFalse(repository.existsByJoinCode("AAAAAA")); // το αρχείο δεν υπάρχει ακόμη

        repository.save(Team.create("Escapers", 42L, "AAAAAA"));

        assertTrue(repository.existsByJoinCode("AAAAAA"));
        assertFalse(repository.existsByJoinCode("ZZZZZZ"));
    }

    @Test
    void idsContinueAfterRestart() {
        String path = tempDir.resolve("teams.txt").toString();
        new FileTeamRepository(path).save(Team.create("A", 1L, "AAAAAA"));

        Team next = new FileTeamRepository(path).save(Team.create("B", 2L, "BBBBBB"));

        assertEquals(2L, next.getId());
    }

    @Test
    void findByJoinCodeLoadsTeamWithMembersAndGreekName() {
        FileTeamRepository repository =
                new FileTeamRepository(tempDir.resolve("teams.txt").toString());
        repository.save(Team.create("Ομάδα | Α", 42L, "AAAAAA"));

        Optional<Team> found = repository.findByJoinCode("AAAAAA");

        assertTrue(found.isPresent());
        assertEquals("Ομάδα | Α", found.get().getName());
        assertTrue(found.get().isCreator(42L));
        assertTrue(repository.findByJoinCode("ZZZZZZ").isEmpty());
    }

    @Test
    void savingExistingTeamUpdatesItInsteadOfDuplicating() {
        String path = tempDir.resolve("teams.txt").toString();
        FileTeamRepository repository = new FileTeamRepository(path);
        repository.save(Team.create("Escapers", 42L, "AAAAAA"));

        Team loaded = repository.findByJoinCode("AAAAAA").orElseThrow();
        loaded.addMember(7L);
        repository.save(loaded);

        Team reloaded = new FileTeamRepository(path).findByJoinCode("AAAAAA").orElseThrow();
        assertEquals(2, reloaded.getMembers().size());
        assertTrue(reloaded.isMember(7L));
        assertEquals(1L, reloaded.getId());
    }

    @Test
    void updatingUnknownTeamFails() {
        FileTeamRepository repository =
                new FileTeamRepository(tempDir.resolve("teams.txt").toString());
        Team ghost = Team.restore(99L, "Ghost", "GGGGGG", List.of());
        assertThrows(IllegalStateException.class, () -> repository.save(ghost));
    }
}
