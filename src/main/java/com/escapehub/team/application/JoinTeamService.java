package com.escapehub.team.application;

import com.escapehub.team.domain.Team;
import com.escapehub.team.domain.TeamRepository;
import org.springframework.stereotype.Service;

/**
 * US-010: joins an existing team using its join code.
 */
@Service
public class JoinTeamService {

    private static final String INVALID_CODE_MESSAGE = "Ο κωδικός συμμετοχής δεν είναι έγκυρος.";
    private static final String ALREADY_MEMBER_MESSAGE = "Είσαι ήδη μέλος αυτής της ομάδας.";
    private static final String JOINED_MESSAGE = "Έγινες μέλος της ομάδας.";

    private final TeamRepository teamRepository;

    /**
     * Creates the use case with its repository dependency.
     *
     * @param teamRepository repository for existing teams
     */
    public JoinTeamService(final TeamRepository teamRepository) {
        if (teamRepository == null) {
            throw new IllegalArgumentException("Team repository must not be null");
        }
        this.teamRepository = teamRepository;
    }

    /**
     * Adds the user to the existing team matching the supplied code.
     *
     * @param userId identifier of the user joining
     * @param code team join code
     * @return a user-facing success or error message
     */
    public String joinTeam(final long userId, final String code) {
        if (code == null || code.isBlank()) {
            return INVALID_CODE_MESSAGE;
        }

        final Team team = teamRepository.findByJoinCode(code).orElse(null);
        if (team == null) {
            return INVALID_CODE_MESSAGE;
        }

        try {
            team.addMember(userId);
        } catch (IllegalStateException exception) {
            return ALREADY_MEMBER_MESSAGE;
        }

        teamRepository.save(team);
        return JOINED_MESSAGE;
    }
}
