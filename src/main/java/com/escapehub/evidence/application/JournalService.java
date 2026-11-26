package com.escapehub.evidence.application;

import com.escapehub.evidence.domain.EvidenceCatalog;
import com.escapehub.evidence.domain.EvidenceEntry;
import com.escapehub.gameplay.application.GameSessionRepository;
import com.escapehub.gameplay.application.SessionParticipantsRepository;
import com.escapehub.gameplay.domain.GameSession;
import com.escapehub.gameplay.domain.SessionParticipants;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Application service for viewing, searching, and adding clues to a session's shared journal.
 */
@Service
public class JournalService {

    private final GameSessionRepository gameSessionRepository;
    private final SessionParticipantsRepository participantsRepository;
    private final EvidenceRepository evidenceRepository;

    /**
     * Creates the journal service backed by session, participant, and evidence repositories.
     *
     * @param gameSessionRepository  repository for game sessions
     * @param participantsRepository repository for session participants
     * @param evidenceRepository     shared repository storing session evidence entries
     */
    public JournalService(
            final GameSessionRepository gameSessionRepository,
            final SessionParticipantsRepository participantsRepository,
            final EvidenceRepository evidenceRepository) {
        this.gameSessionRepository = gameSessionRepository;
        this.participantsRepository = participantsRepository;
        this.evidenceRepository = evidenceRepository;
    }

    /**
     * Returns all shared evidence entries for the session matching an optional search query.
     *
     * @param joinCode session join code
     * @param username authenticated user's username
     * @param query    optional filter text
     * @return matching shared evidence entries
     */
    public List<EvidenceEntry> viewJournal(
            final String joinCode, final String username, final String query) {
        final GameSession session = findSession(joinCode);
        requireActiveParticipantOrCreator(session, username);
        return evidenceRepository.findBySessionId(session.id()).stream()
                .filter(entry -> entry.matches(query))
                .toList();
    }

    /**
     * US-020: Validates eligibility and adds a unique discovered clue to the shared journal.
     *
     * @param joinCode session join code
     * @param username authenticated user's username
     * @param command  clue details to record
     * @return the newly recorded shared evidence entry
     */
    public EvidenceEntry addEvidence(
            final String joinCode,
            final String username,
            final AddEvidenceCommand command) {
        final GameSession session = findSession(joinCode);
        requireActiveParticipantOrCreator(session, username);

        if (command == null || command.evidenceId() == null || command.evidenceId().isBlank()) {
            throw new IllegalArgumentException("Το αναγνωριστικό στοιχείου είναι υποχρεωτικό.");
        }

        final EvidenceCatalog.ClueDefinition definition = EvidenceCatalog
                .findEligible(command.evidenceId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Το στοιχείο δεν είναι επιλέξιμο για προσθήκη στο ημερολόγιο."));

        final boolean alreadyExists = evidenceRepository.findBySessionId(session.id()).stream()
                .anyMatch(existing -> definition.evidenceId().equals(existing.evidenceId()));
        if (alreadyExists) {
            throw new IllegalStateException(
                    "Το στοιχείο έχει ήδη προστεθεί στο κοινό ημερολόγιο της συνεδρίας.");
        }

        final String resolvedTitle = resolveText(command.title(), definition.defaultTitle());
        final String baseDescription = resolveText(
                command.description(), definition.defaultDescription());
        final String resolvedSource = resolveText(command.source(), definition.defaultSource());
        final String fullDescription = baseDescription + " (Πηγή: " + resolvedSource + ")";

        final EvidenceEntry entry = new EvidenceEntry(
                UUID.randomUUID(),
                session.id(),
                definition.evidenceId(),
                resolvedTitle,
                fullDescription,
                Instant.now()
        );

        try {
            return evidenceRepository.save(entry);
        } catch (final DataIntegrityViolationException exception) {
            throw new IllegalStateException(
                    "Το στοιχείο έχει ήδη προστεθεί στο κοινό ημερολόγιο της συνεδρίας.",
                    exception);
        }
    }

    private GameSession findSession(final String joinCode) {
        if (joinCode == null || joinCode.isBlank()) {
            throw new IllegalArgumentException("Ο κωδικός συνεδρίας δεν μπορεί να είναι κενός.");
        }
        return gameSessionRepository.findByJoinCode(joinCode.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new IllegalArgumentException("Μη έγκυρος κωδικός συνεδρίας."));
    }

    private void requireActiveParticipantOrCreator(
            final GameSession session, final String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Απαιτείται σύνδεση χρήστη.");
        }
        final String name = username.trim();
        final boolean isCreator = session.creatorUsername().equalsIgnoreCase(name);
        final Optional<SessionParticipants> participantsOpt =
                participantsRepository.findBySessionId(session.id());

        if (participantsOpt.isPresent()) {
            final SessionParticipants participants = participantsOpt.get();
            final boolean isParticipant = participants.getPlayers().stream()
                    .anyMatch(player -> player.equalsIgnoreCase(name));
            if (!isCreator && !isParticipant) {
                throw new SecurityException("Δεν συμμετέχεις σε αυτή τη συνεδρία.");
            }
            if (!participants.isActive()) {
                throw new IllegalStateException("Η συνεδρία δεν είναι πλέον ενεργή.");
            }
            return;
        }

        if (!isCreator) {
            throw new SecurityException("Δεν συμμετέχεις σε αυτή τη συνεδρία.");
        }
    }
    private String resolveText(final String candidate, final String fallback) {
        if (candidate == null || candidate.isBlank()) {
            return fallback;
        }
        return candidate.trim();
    }
}

