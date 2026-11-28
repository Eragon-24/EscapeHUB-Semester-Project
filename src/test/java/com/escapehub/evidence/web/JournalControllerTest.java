package com.escapehub.evidence.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.escapehub.evidence.application.JournalService;
import com.escapehub.evidence.domain.EvidenceEntry;
import com.escapehub.evidence.infrastructure.InMemoryEvidenceRepository;
import com.escapehub.gameplay.application.CreateGameSessionService;
import com.escapehub.gameplay.application.JoinGameSessionService;
import com.escapehub.gameplay.domain.SessionParticipants;
import com.escapehub.gameplay.domain.GameSession;
import com.escapehub.gameplay.infrastructure.InMemoryGameSessionRepository;
import com.escapehub.gameplay.infrastructure.InMemorySessionParticipantsRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @Test
    @SuppressWarnings("unchecked")
    void addsUniqueEvidenceSharedAcrossParticipantsAndRetainsCreatorAccess() {
        final InMemoryGameSessionRepository sessions = new InMemoryGameSessionRepository();
        final InMemorySessionParticipantsRepository participants =
                new InMemorySessionParticipantsRepository();
        final JournalController controller = new JournalController(new JournalService(
                sessions, participants, new InMemoryEvidenceRepository()));

        final GameSession session = new CreateGameSessionService(sessions).createSession("alice");
        new JoinGameSessionService(sessions, participants).joinSession(session.joinCode(), "bob");

        final ResponseEntity<?> created = controller.addEvidence(
                session.joinCode(),
                new JournalController.AddEvidenceRequest("LAB_NOTEBOOK", null, null, null),
                () -> "bob"
        );
        assertEquals(HttpStatus.CREATED, created.getStatusCode());

        // Duplicate evidenceId in same session -> 409 Conflict
        final ResponseEntity<?> duplicate = controller.addEvidence(
                session.joinCode(),
                new JournalController.AddEvidenceRequest(
                        "LAB_NOTEBOOK", "Διαφορετικός Τίτλος", null, null),
                () -> "alice"
        );
        assertEquals(HttpStatus.CONFLICT, duplicate.getStatusCode());

        // Ineligible clue -> 400 Bad Request
        assertEquals(HttpStatus.BAD_REQUEST,
                controller.addEvidence(
                        session.joinCode(),
                        new JournalController.AddEvidenceRequest("FAKE_CLUE", null, null, null),
                        () -> "alice"
                ).getStatusCode());

        // Creator leaves active player list -> creator still retains access to own session
        final SessionParticipants sessionParticipants =
                participants.findBySessionId(session.id()).orElseThrow();
        sessionParticipants.removePlayer("alice");
        participants.save(sessionParticipants);

        final ResponseEntity<?> viewedByCreator =
                controller.getJournal(session.joinCode(), null, () -> "alice");
        assertEquals(HttpStatus.OK, viewedByCreator.getStatusCode());
        final List<EvidenceEntry> entries = (List<EvidenceEntry>) viewedByCreator.getBody();
        assertEquals(1, entries.size());
        assertEquals("LAB_NOTEBOOK", entries.get(0).evidenceId());
    }
}