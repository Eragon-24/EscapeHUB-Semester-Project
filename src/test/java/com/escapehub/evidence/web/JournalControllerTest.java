package com.escapehub.evidence.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.escapehub.evidence.application.JournalService;
import com.escapehub.evidence.infrastructure.InMemoryEvidenceRepository;
import com.escapehub.gameplay.application.CreateGameSessionService;
import com.escapehub.gameplay.domain.GameSession;
import com.escapehub.gameplay.infrastructure.InMemoryGameSessionRepository;
import com.escapehub.gameplay.infrastructure.InMemorySessionParticipantsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.ui.ExtendedModelMap;

class JournalControllerTest {

    @Test
    void mapsResultsToHttpStatusesAndView() {
        final InMemoryGameSessionRepository sessions = new InMemoryGameSessionRepository();
        final JournalController controller = new JournalController(new JournalService(
                sessions, new InMemorySessionParticipantsRepository(),
                new InMemoryEvidenceRepository()));
        final GameSession session = new CreateGameSessionService(sessions).createSession("alice");

        assertEquals(HttpStatus.OK,
                controller.getJournal(session.joinCode(), null, () -> "alice").getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN,
                controller.getJournal(session.joinCode(), "x", () -> "eve").getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND,
                controller.getJournal("ZZZZZZ", null, () -> "alice").getStatusCode());
        assertEquals("journal",
                controller.showJournal(session.joinCode(), () -> "alice", new ExtendedModelMap()));
    }
}
