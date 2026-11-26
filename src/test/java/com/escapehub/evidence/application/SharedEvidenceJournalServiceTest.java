package com.escapehub.evidence.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.escapehub.evidence.domain.EvidenceEntry;
import com.escapehub.evidence.infrastructure.InMemoryEvidenceRepository;
import com.escapehub.gameplay.application.CreateGameSessionService;
import com.escapehub.gameplay.application.JoinGameSessionService;
import com.escapehub.gameplay.domain.GameSession;
import com.escapehub.gameplay.domain.SessionParticipants;
import com.escapehub.gameplay.infrastructure.InMemoryGameSessionRepository;
import com.escapehub.gameplay.infrastructure.InMemorySessionParticipantsRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SharedEvidenceJournalServiceTest {

    private CreateGameSessionService createSessionService;
    private JoinGameSessionService joinSessionService;
    private InMemorySessionParticipantsRepository participantsRepository;
    private SharedEvidenceJournalService journalService;
    private GameSession session;

    @BeforeEach
    void setUp() {
        final InMemoryGameSessionRepository sessionRepository = new InMemoryGameSessionRepository();
        participantsRepository = new InMemorySessionParticipantsRepository();
        createSessionService = new CreateGameSessionService(sessionRepository);
        joinSessionService = new JoinGameSessionService(sessionRepository, participantsRepository);
        journalService = new SharedEvidenceJournalService(
                new InMemoryEvidenceRepository(),
                sessionRepository,
                participantsRepository
        );
        session = createSessionService.createSession("player1");
        joinSessionService.joinSession(session.joinCode(), "player2");
    }

    @Test
    void shouldAddEligibleEvidenceAndShareAcrossAllSessionParticipants() {
        final EvidenceEntry added = journalService.addEvidence(
                session.id(),
                "player1",
                new AddEvidenceCommand("LAB_NOTEBOOK", null, null, null)
        );

        assertEquals(session.id(), added.sessionId());
        assertEquals("Σημειωματάριο Εργαστηρίου", added.title());
        assertTrue(added.description().contains("Πάγκος Εργαστηρίου"));

        final List<EvidenceEntry> seenByPlayer2 = journalService
                .getJournalEntries(session.id(), "player2");
        assertEquals(1, seenByPlayer2.size());
        assertEquals(added, seenByPlayer2.get(0));
    }

    @Test
    void shouldIsolateJournalsBetweenDifferentSessions() {
        final GameSession otherSession = createSessionService.createSession("otherPlayer");

        journalService.addEvidence(
                session.id(),
                "player1",
                new AddEvidenceCommand("LAB_NOTEBOOK", null, null, null)
        );

        assertTrue(journalService.getJournalEntries(otherSession.id(), "otherPlayer").isEmpty());
    }

    @Test
    void shouldRejectDuplicateEvidenceInSameSession() {
        journalService.addEvidence(
                session.id(),
                "player1",
                new AddEvidenceCommand("CHEMICAL_FORMULA", null, null, null)
        );

        assertThrows(
                IllegalStateException.class,
                () -> journalService.addEvidence(
                        session.id(),
                        "player2",
                        new AddEvidenceCommand("CHEMICAL_FORMULA", null, null, null)
                )
        );
    }

    @Test
    void shouldRejectIneligibleEvidenceId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> journalService.addEvidence(
                        session.id(),
                        "player1",
                        new AddEvidenceCommand("UNKNOWN_FAKE_CLUE", null, null, null)
                )
        );
    }

    @Test
    void shouldRejectNonParticipantAndUnknownOrInactiveSessions() {
        assertThrows(
                SecurityException.class,
                () -> journalService.addEvidence(
                        session.id(),
                        "intruder",
                        new AddEvidenceCommand("LAB_NOTEBOOK", null, null, null)
                )
        );
        assertThrows(
                SecurityException.class,
                () -> journalService.getJournalEntries(session.id(), "intruder")
        );
        assertThrows(
                NoSuchElementException.class,
                () -> journalService.getJournalEntries(UUID.randomUUID(), "player1")
        );

        final SessionParticipants participants = participantsRepository
                .findBySessionId(session.id())
                .orElseThrow();
        participants.deactivate();
        participantsRepository.save(participants);

        assertThrows(
                IllegalStateException.class,
                () -> journalService.addEvidence(
                        session.id(),
                        "player1",
                        new AddEvidenceCommand("LAB_NOTEBOOK", null, null, null)
                )
        );
    }
}