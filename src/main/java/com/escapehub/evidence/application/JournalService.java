package com.escapehub.evidence.application;

import com.escapehub.evidence.domain.EvidenceEntry;
import com.escapehub.gameplay.application.GameSessionRepository;
import com.escapehub.gameplay.application.SessionParticipantsRepository;
import com.escapehub.gameplay.domain.GameSession;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class JournalService {

    private final GameSessionRepository gameSessionRepository;
    private final SessionParticipantsRepository participantsRepository;
    private final EvidenceRepository evidenceRepository;

    public JournalService(
            final GameSessionRepository gameSessionRepository,
            final SessionParticipantsRepository participantsRepository,
            final EvidenceRepository evidenceRepository) {
        this.gameSessionRepository = gameSessionRepository;
        this.participantsRepository = participantsRepository;
        this.evidenceRepository = evidenceRepository;
    }

    public List<EvidenceEntry> viewJournal(
            final String joinCode, final String username, final String query) {
        final GameSession session = findSession(joinCode);
        requireParticipant(session, username);
        return evidenceRepository.findBySessionId(session.id()).stream()
                .filter(entry -> entry.matches(query))
                .toList();
    }

    private GameSession findSession(final String joinCode) {
        if (joinCode == null || joinCode.isBlank()) {
            throw new IllegalArgumentException("Ο κωδικός συνεδρίας δεν μπορεί να είναι κενός.");
        }
        return gameSessionRepository.findByJoinCode(joinCode.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new IllegalArgumentException("Μη έγκυρος κωδικός συνεδρίας."));
    }

    private void requireParticipant(final GameSession session, final String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Απαιτείται σύνδεση χρήστη.");
        }
        final String name = username.trim();
        if (session.creatorUsername().equalsIgnoreCase(name)) {
            return;
        }
        final boolean isMember = participantsRepository.findBySessionId(session.id())
                .map(p -> p.getPlayers().stream().anyMatch(player -> player.equalsIgnoreCase(name)))
                .orElse(false);
        if (!isMember) {
            throw new SecurityException("Δεν συμμετέχεις σε αυτή τη συνεδρία.");
        }
    }
}