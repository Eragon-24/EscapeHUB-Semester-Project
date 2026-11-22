package com.escapehub.test.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.escapehub.team.application.TeamService;
import com.escapehub.team.domain.DuplicateJoinCodeException;
import com.escapehub.team.domain.Team;
import com.escapehub.team.domain.TeamRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TeamServiceTest {

    /** Ψεύτικη βάση στη μνήμη, μόνο για τα tests. */
    private static class InMemoryTeamRepository implements TeamRepository {
        private final List<Team> saved = new ArrayList<>();
        private int codesReportedAsTaken;
        private int duplicateSaveAttempts;

        @Override
        public Team save(Team team) {
            if (duplicateSaveAttempts > 0) {
                duplicateSaveAttempts--;
                throw new DuplicateJoinCodeException(team.getJoinCode());
            }
            if (saved.stream().anyMatch(existing -> existing.getJoinCode().equals(team.getJoinCode()))) {
                throw new DuplicateJoinCodeException(team.getJoinCode());
            }
            team.setId((long) saved.size() + 1);
            saved.add(team);
            return team;
        }

        @Override
        public boolean existsByJoinCode(String joinCode) {
            if (codesReportedAsTaken > 0) {
                codesReportedAsTaken--;
                return true;
            }
            return saved.stream().anyMatch(t -> t.getJoinCode().equals(joinCode));
        }

        @Override
        public Optional<Team> findByJoinCode(String joinCode) {
            return saved.stream().filter(t -> t.getJoinCode().equals(joinCode)).findFirst();
        }
    }

    private final InMemoryTeamRepository repository = new InMemoryTeamRepository();
    private final TeamService service = new TeamService(repository);

    @Test
    void creatorBecomesMemberAndCreator() {
        Team team = service.createTeam(42L, "Escapers");
        assertTrue(team.isMember(42L));
        assertTrue(team.isCreator(42L));
        assertEquals(1, team.getMembers().size());
    }

    @Test
    void teamIsSavedWithIdAndTrimmedName() {
        Team team = service.createTeam(1L, "  Escapers  ");
        assertNotNull(team.getId());
        assertEquals("Escapers", team.getName());
        assertEquals(1, repository.saved.size());
    }

    @Test
    void joinCodeHasSixCharactersAndIsRetriedWhenTaken() {
        repository.codesReportedAsTaken = 3;
        Team team = service.createTeam(1L, "Escapers");
        assertEquals(TeamService.JOIN_CODE_LENGTH, team.getJoinCode().length());
    }

    @Test
    void retriesWhenJoinCodeIsClaimedBeforeSave() {
        repository.duplicateSaveAttempts = 1;

        Team team = service.createTeam(1L, "Escapers");

        assertNotNull(team.getId());
        assertEquals(1, repository.saved.size());
    }

    @Test
    void twoTeamsGetDifferentJoinCodes() {
        Team first = service.createTeam(1L, "A");
        Team second = service.createTeam(2L, "B");
        assertTrue(!first.getJoinCode().equals(second.getJoinCode()));
    }

    @Test
    void invalidNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.createTeam(1L, "   "));
        assertThrows(IllegalArgumentException.class, () -> service.createTeam(1L, null));
        assertThrows(IllegalArgumentException.class,
                () -> service.createTeam(1L, "x".repeat(TeamService.MAX_NAME_LENGTH + 1)));
    }
}
