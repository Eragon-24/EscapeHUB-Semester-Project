package com.escapehub.gameplay.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.escapehub.team.application.LeaveTeamService;
import com.escapehub.team.domain.Team;
import com.escapehub.team.infrastructure.FileTeamRepository;
import java.nio.file.Path;

import com.escapehub.team.web.LeaveTeamController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

class LeaveTeamControllerTest {

    @TempDir
    Path tempDir;

    private FileTeamRepository repository;
    private LeaveTeamController controller;
    private static final String JOIN_CODE = "AAAAAA";

    @BeforeEach
    void setUp() {
        repository = new FileTeamRepository(tempDir.resolve("teams.txt").toString());
        controller = new LeaveTeamController(new LeaveTeamService(repository));

        Team team = Team.create("Escapers", 1L, JOIN_CODE);
        team.addMember(2L);
        repository.save(team);
    }

    @Test
    void leavingRemovesTheLoggedInUserAndRedirectsToTheMultiplayerRoom() {
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = controller.leave(JOIN_CODE, () -> "2", redirect);

        assertEquals(
                "redirect:" + LeaveTeamController.MULTIPLAYER_ROOM_PATH,
                view);
        assertFalse(
                redirect.getFlashAttributes().get("message").toString().isBlank());
        assertFalse(
                repository.findByJoinCode(JOIN_CODE)
                        .orElseThrow()
                        .isMember(2L));
    }

    @Test
    void requestWithoutLoginIsRejectedAndChangesNothing() {
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        assertThrows(
                ResponseStatusException.class,
                () -> controller.leave(JOIN_CODE, null, redirect));

        assertTrue(
                repository.findByJoinCode(JOIN_CODE)
                        .orElseThrow()
                        .isMember(2L));
    }
}