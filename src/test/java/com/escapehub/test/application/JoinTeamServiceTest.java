package com.escapehub.test.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.escapehub.team.application.JoinTeamService;
import com.escapehub.team.domain.Team;
import com.escapehub.team.domain.TeamRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class JoinTeamServiceTest {

    private static final String JOINED_MESSAGE = "Έγινες μέλος της ομάδας.";
    private static final String INVALID_CODE_MESSAGE = "Ο κωδικός συμμετοχής δεν είναι έγκυρος.";
    private static final String ALREADY_MEMBER_MESSAGE = "Είσαι ήδη μέλος αυτής της ομάδας.";

    private static class InMemoryTeamRepository implements TeamRepository {
        private Team team;
        private String lastRequestedCode;
        private int saveCalls;

        @Override
        public Team save(final Team teamToSave) {
            saveCalls++;
            team = teamToSave;
            return teamToSave;
        }

        @Override
        public boolean existsByJoinCode(final String joinCode) {
            return team != null && team.getJoinCode().equals(joinCode);
        }

        @Override
        public Optional<Team> findByJoinCode(final String joinCode) {
            lastRequestedCode = joinCode;
            if (team != null && team.getJoinCode().equals(joinCode)) {
                return Optional.of(team);
            }
            return Optional.empty();
        }
    }

    @Test
    void joinsTeamAndNormalizesJoinCode() {
        final InMemoryTeamRepository repository = new InMemoryTeamRepository();
        repository.team = Team.create("Escapers", 10L, "AB23CD");
        final JoinTeamService service = new JoinTeamService(repository);

        final String result = service.joinTeam(20L, "  ab23cd  ");

        assertEquals(JOINED_MESSAGE, result);
        assertEquals("AB23CD", repository.lastRequestedCode);
        assertTrue(repository.team.isMember(20L));
        assertFalse(repository.team.isCreator(20L));
        assertEquals(1, repository.saveCalls);
    }

    @Test
    void rejectsNullOrBlankJoinCodesWithoutRepositoryLookup() {
        final InMemoryTeamRepository repository = new InMemoryTeamRepository();
        final JoinTeamService service = new JoinTeamService(repository);

        assertEquals(INVALID_CODE_MESSAGE, service.joinTeam(20L, null));
        assertEquals(INVALID_CODE_MESSAGE, service.joinTeam(20L, "   "));

        assertNull(repository.lastRequestedCode);
        assertEquals(0, repository.saveCalls);
    }

    @Test
    void rejectsUnknownJoinCodeWithoutSaving() {
        final InMemoryTeamRepository repository = new InMemoryTeamRepository();
        repository.team = Team.create("Escapers", 10L, "AB23CD");
        final JoinTeamService service = new JoinTeamService(repository);

        assertEquals(INVALID_CODE_MESSAGE, service.joinTeam(20L, "ZZ99ZZ"));

        assertEquals("ZZ99ZZ", repository.lastRequestedCode);
        assertEquals(0, repository.saveCalls);
        assertFalse(repository.team.isMember(20L));
    }

    @Test
    void returnsAlreadyMemberMessageWithoutSavingAgain() {
        final InMemoryTeamRepository repository = new InMemoryTeamRepository();
        repository.team = Team.create("Escapers", 10L, "AB23CD");
        final JoinTeamService service = new JoinTeamService(repository);

        assertEquals(ALREADY_MEMBER_MESSAGE, service.joinTeam(10L, "AB23CD"));

        assertEquals(1, repository.team.getMembers().size());
        assertEquals(0, repository.saveCalls);
    }

    @Test
    void rejectsNullRepository() {
        assertThrows(IllegalArgumentException.class, () -> new JoinTeamService(null));
    }
}
