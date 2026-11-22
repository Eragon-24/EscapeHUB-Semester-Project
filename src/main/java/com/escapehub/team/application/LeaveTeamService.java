package com.escapehub.team.application;

import com.escapehub.team.domain.Team;
import com.escapehub.team.domain.TeamRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * US-014: ένας συμμετέχων αποχωρεί από την ομάδα του.
 *
 * <p>Χρήση (ο currentUserId να είναι ο συνδεδεμένος χρήστης, ΠΟΤΕ από το αίτημα του client):
 * <pre>
 * LeaveResult result = leaveTeamService.leaveTeam(currentUserId, team.getJoinCode());
 * if (result.left()) { // redirect στο Multiplayer Room και εμφάνιση του result.message() }
 * </pre>
 */
@Service
public class LeaveTeamService {

    /** Τι έγινε. */
    public enum LeaveStatus { LEFT, NOT_A_MEMBER, CREATOR_CANNOT_LEAVE, TEAM_NOT_FOUND }

    /** Το αποτέλεσμα, με μήνυμα έτοιμο για εμφάνιση στον χρήστη. */
    public record LeaveResult(LeaveStatus status, String message) {

        public boolean left() {
            return status == LeaveStatus.LEFT;
        }
    }

    private final TeamRepository repository;

    public LeaveTeamService(TeamRepository repository) {
        this.repository = repository;
    }

    /**
     * Αφαιρεί ΜΟΝΟ τον χρήστη που κάνει το αίτημα από την ομάδα με αυτόν τον κωδικό
     * συμμετοχής. Η ομάδα, ο κωδικός και οι υπόλοιποι συμμετέχοντες μένουν όπως είναι.
     * Αν ο χρήστης δεν είναι μέλος, η ομάδα δεν υπάρχει ή ο χρήστης είναι ο δημιουργός,
     * δεν αλλάζει τίποτα και δεν πετιέται σφάλμα. Ο δημιουργός δεν αποχωρεί, γιατί η
     * μεταβίβαση ή η διάλυση ομάδας δεν ορίζονται ακόμη.
     */
    public LeaveResult leaveTeam(long currentUserId, String joinCode) {
        Optional<Team> found = repository.findByJoinCode(joinCode);
        if (found.isEmpty()) {
            return new LeaveResult(LeaveStatus.TEAM_NOT_FOUND, "Η ομάδα δεν βρέθηκε.");
        }
        Team team = found.get();
        if (team.isCreator(currentUserId)) {
            return new LeaveResult(LeaveStatus.CREATOR_CANNOT_LEAVE,
                    "Ο δημιουργός δεν μπορεί να αποχωρήσει από την ομάδα.");
        }
        if (!team.removeMember(currentUserId)) {
            return new LeaveResult(LeaveStatus.NOT_A_MEMBER,
                    "Δεν είσαι μέλος αυτής της ομάδας.");
        }
        repository.save(team);
        return new LeaveResult(LeaveStatus.LEFT, "Αποχώρησες από την ομάδα.");
    }
}