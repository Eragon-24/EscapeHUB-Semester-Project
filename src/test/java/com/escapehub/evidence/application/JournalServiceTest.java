package com.escapehub.evidence.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.escapehub.evidence.domain.EvidenceEntry;
import com.escapehub.evidence.infrastructure.InMemoryEvidenceRepository;
import com.escapehub.gameplay.application.CreateGameSessionService;
import com.escapehub.gameplay.application.JoinGameSessionService;
import com.escapehub.gameplay.domain.GameSession;
import com.escapehub.gameplay.infrastructure.InMemoryGameSessionRepository;
import com.escapehub.gameplay.infrastructure.InMemorySessionParticipantsRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JournalServiceTest {

    private CreateGameSessionService createService;
    private JoinGameSessionService joinService;
    private InMemoryEvidenceRepository evidence;
    private JournalService journal;

    @BeforeEach
    void setUp() {
        final InMemoryGameSessionRepository sessions = new InMemoryGameSessionRepository();
        final InMemorySessionParticipantsRepository participants =
                new InMemorySessionParticipantsRepository();
        createService = new CreateGameSessionService(sessions);
        joinService = new JoinGameSessionService(sessions, participants);
        evidence = new InMemoryEvidenceRepository();
        journal = new JournalService(sessions, participants, evidence);
    }

    private void add(final GameSession session, final String title, final String description) {
        evidence.save(new EvidenceEntry(
                UUID.randomUUID(), session.id(), title, description, Instant.now()));
    }

    @Test
    void allParticipantsSeeTheSameEntries() {
        final GameSession session = createService.createSession("alice");
        joinService.joinSession(session.joinCode(), "bob");
        add(session, "Σημειωματάριο", "Κωδικός 483920");
        add(session, "Σεντούκι", "Κλειδωμένο");

        final List<EvidenceEntry> forAlice = journal.viewJournal(session.joinCode(), "alice", null);
        final List<EvidenceEntry> forBob = journal.viewJournal(session.joinCode(), "bob", "");

        assertEquals(2, forAlice.size());
        assertEquals(forAlice, forBob);
    }

    @Test
    void searchReturnsOnlyMatchingEntriesOfThisSession() {
        final GameSession mine = createService.createSession("alice");
        final GameSession other = createService.createSession("carol");
        add(mine, "Σημειωματάριο", "Κωδικός");
        add(mine, "Σεντούκι", "Κλειδωμένο");
        add(other, "Σημειωματάριο άλλης ομάδας", "Μυστικό");

        final List<EvidenceEntry> result = journal.viewJournal(mine.joinCode(), "alice", "σημειωμ");

        assertEquals(1, result.size());
        assertEquals(mine.id(), result.get(0).sessionId());
        assertEquals(1, journal.viewJournal(mine.joinCode(), "alice", "κλειδωμενο").size());
    }

    @Test
    void newEvidenceAppearsOnNextLoad() {
        final GameSession session = createService.createSession("alice");
        assertEquals(0, journal.viewJournal(session.joinCode(), "alice", null).size());

        add(session, "Νέα ένδειξη", "περιγραφή");

        assertEquals(1, journal.viewJournal(session.joinCode(), "alice", null).size());
    }

    @Test
    void participantCannotViewAnotherSessionsJournal() {
        final GameSession mine = createService.createSession("alice");
        final GameSession other = createService.createSession("carol");
        add(other, "Μυστικό", "x");

        assertThrows(SecurityException.class,
                () -> journal.viewJournal(other.joinCode(), "alice", null));
        assertThrows(SecurityException.class,
                () -> journal.viewJournal(mine.joinCode(), "carol", null));
    }

    @Test
    void rejectsUnknownOrBlankJoinCode() {
        assertThrows(IllegalArgumentException.class,
                () -> journal.viewJournal("ZZZZZZ", "alice", null));
        assertThrows(IllegalArgumentException.class,
                () -> journal.viewJournal(" ", "alice", null));
    }
}
