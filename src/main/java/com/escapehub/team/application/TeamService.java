package com.escapehub.team.application;

import com.escapehub.team.domain.Team;
import com.escapehub.team.domain.TeamRepository;
import java.security.SecureRandom;
import org.springframework.stereotype.Service;

/**
 * US-009: δημιουργία ομάδας.
 *
 * <p>Χρήση (ο userId είναι του συνδεδεμένου χρήστη, από το login):
 * <pre>
 * Team team = teamService.createTeam(currentUserId, "Escapers");
 * team.getJoinCode();   // κωδικός συμμετοχής για τους υπόλοιπους
 * </pre>
 */
@Service
public class TeamService {

    public static final int MAX_NAME_LENGTH = 50;
    static final int JOIN_CODE_LENGTH = 6;

    // Χωρίς 0/O και 1/I για να μη μπερδεύονται.
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final TeamRepository repository;
    private final SecureRandom random = new SecureRandom();

    public TeamService(TeamRepository repository) {
        this.repository = repository;
    }

    /**
     * Δημιουργεί ομάδα: ο δημιουργός γίνεται μέλος και αρχηγός, η ομάδα παίρνει
     * μοναδικό κωδικό συμμετοχής και όλα αποθηκεύονται μέσω του repository.
     *
     * @throws IllegalArgumentException αν το όνομα είναι κενό ή πολύ μεγάλο
     */
    public Team createTeam(long creatorUserId, String name) {
        String cleanName = name == null ? "" : name.trim();
        if (cleanName.isEmpty() || cleanName.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "Team name must be 1-" + MAX_NAME_LENGTH + " characters");
        }
        Team team = Team.create(cleanName, creatorUserId, generateUniqueJoinCode());
        return repository.save(team);
    }

    private String generateUniqueJoinCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(JOIN_CODE_LENGTH);
            for (int i = 0; i < JOIN_CODE_LENGTH; i++) {
                sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
            }
            code = sb.toString();
        } while (repository.existsByJoinCode(code));
        return code;
    }
}