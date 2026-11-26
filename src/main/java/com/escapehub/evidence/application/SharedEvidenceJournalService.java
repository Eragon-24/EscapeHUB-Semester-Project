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
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * US-020: Application service for adding discovered clues to a session's shared journal
 * and retrieving the shared collection for active participants.
 */
@Service
public class SharedEvidenceJournalService {

    private final EvidenceRepository evidenceRepository;
    private final GameSessionRepository gameSessionRepository;
    private final SessionParticipantsRepository participantsRepository;

    /**
     * Creates the service backed by evidence, session, and participant repositories.
     *
     * @param evidenceRepository shared repository storing session evidence entries
     * @param gameSessionRepository repository for game sessions
     * @param participantsRepository repository for session participants
     */
    public SharedEvidenceJournalService(
            final EvidenceRepository evidenceRepository,
            final GameSessionRepository gameSessionRepository,
            final SessionParticipantsRepository participantsRepository) {
        this.evidenceRepository = Objects.requireNonNull(
                evidenceRepository, "Evidence repository must not be null");
        this.gameSessionRepository = Objects.requireNonNull(
                gameSessionRepository, "Game session repository must not be null");
        this.participantsRepository = Objects.requireNonNull(
                participantsRepository, "Participants repository must not be null");
    }

    /**
     * Validates eligibility and adds a discovered clue to the current session's shared journal.
     *
     * @param sessionId unique session identifier
     * @param username authenticated participant's username
     * @param command evidence details to add
     * @return the newly recorded shared evidence entry
     */
    public synchronized EvidenceEntry addEvidence(
            final UUID sessionId,
            final String username,
            final AddEvidenceCommand command) {
        requireActiveParticipant(sessionId, username);
        if (command == null || command.evidenceId() == null || command.evidenceId().isBlank()) {
            throw new IllegalArgumentException("Το αναγνωριστικό στοιχείου είναι υποχρεωτικό.");
        }

        final EvidenceCatalog.ClueDefinition definition = EvidenceCatalog
                .findEligible(command.evidenceId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Το στοιχείο δεν είναι επιλέξιμο για προσθήκη στο ημερολόγιο."));

        final String resolvedTitle = resolveText(command.title(), definition.defaultTitle());
        final String baseDescription = resolveText(
                command.description(), definition.defaultDescription());
        final String resolvedSource = resolveText(command.source(), definition.defaultSource());
        final String fullDescription = baseDescription + " (Πηγή: " + resolvedSource + ")";

        final boolean alreadyExists = evidenceRepository.findBySessionId(sessionId).stream()
                .anyMatch(existing -> existing.title().trim().toLowerCase(Locale.ROOT)
                        .equals(resolvedTitle.toLowerCase(Locale.ROOT)));
        if (alreadyExists) {
            throw new IllegalStateException(
                    "Το στοιχείο έχει ήδη προστεθεί στο κοινό ημερολόγιο της συνεδρίας.");
        }

        final EvidenceEntry entry = new EvidenceEntry(
                UUID.randomUUID(),
                sessionId,
                resolvedTitle,
                fullDescription,
                Instant.now()
        );

        return evidenceRepository.save(entry);
    }

    /**
     * Returns all shared evidence entries recorded for the participant's current session.
     *
     * @param sessionId unique session identifier
     * @param username authenticated participant's username
     * @return list of shared evidence entries
     */
    public synchronized List<EvidenceEntry> getJournalEntries(
            final UUID sessionId,
            final String username) {
        requireActiveParticipant(sessionId, username);
        return evidenceRepository.findBySessionId(sessionId);
    }

    private void requireActiveParticipant(final UUID sessionId, final String username) {
        Objects.requireNonNull(sessionId, "Session id must not be null");
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Απαιτείται σύνδεση χρήστη.");
        }

        final GameSession session = gameSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Η συνεδρία δεν βρέθηκε."));
        final String cleanUsername = username.trim();
        final Optional<SessionParticipants> participantsOptional =
                participantsRepository.findBySessionId(sessionId);

        if (participantsOptional.isPresent()) {
            final SessionParticipants participants = participantsOptional.get();
            if (!participants.isActive()) {
                throw new IllegalStateException("Η συνεδρία δεν είναι πλέον ενεργή.");
            }
            if (!participants.getPlayers().contains(cleanUsername)) {
                throw new SecurityException("Δεν συμμετέχεις σε αυτή τη συνεδρία.");
            }
            return;
        }

        if (!session.creatorUsername().equals(cleanUsername)) {
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