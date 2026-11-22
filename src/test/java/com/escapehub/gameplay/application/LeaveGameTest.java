package com.escapehub.gameplay.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class LeaveGameTest {

    @Test
    void leavingSendsThePlayerToTheMainMenuWithAMessage() {
        LeaveGame.LeaveResult result = new LeaveGame().leave(7L, 100L);

        assertEquals(LeaveGame.MAIN_MENU_PATH, result.redirectTo());
        assertFalse(result.message().isBlank());
    }

    @Test
    void leavingCallsTheSessionHookForTheCurrentUserOnly() {
        List<String> calls = new ArrayList<>();
        LeaveGame leaveGame = new LeaveGame() {
            @Override
            protected void removeFromSession(long userId, long sessionId) {
                calls.add(userId + "@" + sessionId);
            }
        };

        leaveGame.leave(7L, 100L);

        assertEquals(List.of("7@100"), calls);
    }
}
