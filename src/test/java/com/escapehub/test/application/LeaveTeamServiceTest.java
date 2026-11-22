package com.escapehub.test.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.escapehub.team.application.LeaveTeamService;
import com.escapehub.team.application.LeaveTeamService.LeaveResult;
import com.escapehub.team.application.LeaveTeamService.LeaveStatus;
import com.escapehub.team.domain.Team;
import com.escapehub.team.infrastructure.FileTeamRepository;
import java.nio.file.Path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LeaveTeamServiceTest {

    private static final long CREATOR = 1L;
    private static final long PLAYER = 2L;
    private static final long OTHER_PLAYER = 3L;
    private static final long STRANGER = 99L;

    @TempDir
    Path tempDir;

    private FileTeamRepository repository;
    private LeaveTeamService service;
    private static final String JOIN_CODE = "AAAAAA";
    private Assertions Assertions;

    @BeforeEach
    void setUp() {
        repository = new FileTeamRepository(tempDir.resolve("teams.txt").toString());
        service = new LeaveTeamService(repository);
        Team team = Team.create("Escapers", CREATOR, JOIN_CODE);
        team.addMember(PLAYER);
        team.addMember(OTHER_PLAYER);
        repository.save(team);
    }

    private Team stored() {
        return repository.findByJoinCode(JOIN_CODE).orElseThrow();
    }

    @Test
    void memberLeavesAndIsRemovedFromTheStoredTeam() {
        LeaveResult result = service.leaveTeam(PLAYER, JOIN_CODE);

        Assertions.assertTrue(result.left());
        Assertions.assertFalse(result.message().isBlank());
        Assertions.assertFalse(stored().isMember(PLAYER));
        Assertions.assertEquals(2, stored().getMembers().size());
    }

    @Test
    void teamAndOtherMembersRemainIntact() {
        service.leaveTeam(PLAYER, JOIN_CODE);

        Team team = stored();
        Assertions.assertEquals("Escapers", team.getName());
        Assertions.assertEquals(JOIN_CODE, team.getJoinCode());
        Assertions.assertTrue(team.isCreator(CREATOR));
        Assertions.assertTrue(team.isMember(OTHER_PLAYER));
    }

    @Test
    void memberCanJoinAgainWithTheSameJoinCode() {
        service.leaveTeam(PLAYER, JOIN_CODE);

        Team team = stored();
        team.addMember(PLAYER);
        repository.save(team);

        Assertions.assertTrue(stored().isMember(PLAYER));
    }

    @Test
    void userWhoIsNotAMemberChangesNothing() {
        LeaveResult result = service.leaveTeam(STRANGER, JOIN_CODE);

        Assertions.assertEquals(LeaveStatus.NOT_A_MEMBER, result.status());
        Assertions.assertEquals(3, stored().getMembers().size());
    }

    @Test
    void leavingTwiceIsHarmless() {
        service.leaveTeam(PLAYER, JOIN_CODE);

        LeaveResult second = service.leaveTeam(PLAYER, JOIN_CODE);

        Assertions.assertEquals(LeaveStatus.NOT_A_MEMBER, second.status());
        Assertions.assertEquals(2, stored().getMembers().size());
    }

    @Test
    void creatorCannotLeave() {
        LeaveResult result = service.leaveTeam(CREATOR, JOIN_CODE);

        Assertions.assertEquals(LeaveStatus.CREATOR_CANNOT_LEAVE, result.status());
        Assertions.assertTrue(stored().isCreator(CREATOR));
        Assertions.assertEquals(3, stored().getMembers().size());
    }

    @Test
    void unknownTeamIsHandledSafely() {
        LeaveResult result = service.leaveTeam(PLAYER, "ZZZZZZ");

        Assertions.assertEquals(LeaveStatus.TEAM_NOT_FOUND, result.status());
        Assertions.assertFalse(result.left());
    }
}