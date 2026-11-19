package com.escapehub.test.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.escapehub.team.domain.Team;
import org.junit.jupiter.api.Test;

class TeamTest {

    private final Team team = Team.create("Escapers", 42L, "AAAAAA");

    @Test
    void creatorIsMemberWithCreatorRole() {
        assertTrue(team.isMember(42L));
        assertTrue(team.isCreator(42L));
    }

    @Test
    void addMemberAddsNormalMember() {
        team.addMember(7L);
        assertTrue(team.isMember(7L));
        assertFalse(team.isCreator(7L));
        assertEquals(2, team.getMembers().size());
    }

    @Test
    void addingSameUserTwiceIsRejected() {
        team.addMember(7L);
        assertThrows(IllegalStateException.class, () -> team.addMember(7L));
        assertThrows(IllegalStateException.class, () -> team.addMember(42L));
    }
}