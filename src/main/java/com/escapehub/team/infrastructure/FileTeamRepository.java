package com.escapehub.team.infrastructure;

import com.escapehub.team.domain.DuplicateJoinCodeException;
import com.escapehub.team.domain.Team;
import com.escapehub.team.domain.TeamRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
/**
 * Προσωρινή αποθήκευση ομάδων σε αρχείο txt (μέχρι να υπάρξει βάση δεδομένων).
 *
 * <p>Μία γραμμή ανά ομάδα: {@code id|όνομα|κωδικός|userId:ΡΟΛΟΣ,userId:ΡΟΛΟΣ}
 * Το όνομα αποθηκεύεται κωδικοποιημένο (URL encoding), ώστε να αντέχει ελληνικά και
 * ειδικούς χαρακτήρες.
 *
 * <p>Όταν έρθει η βάση: φτιάχνεται νέα κλάση που υλοποιεί TeamRepository και αυτή η
 * κλάση σβήνεται (ή χάνει το @Repository). Ο υπόλοιπος κώδικας δεν αλλάζει.
 */
@Repository
@Profile("local")
public class FileTeamRepository implements TeamRepository {

    private final Path file;

    public FileTeamRepository(@Value("${escapehub.teams.file:data/teams.txt}") String filePath) {
        this.file = Path.of(filePath);
    }

    @Override
    public synchronized Team save(Team team) {
        List<Team> teams = readAll();
        boolean duplicateJoinCode = teams.stream()
                .filter(existing -> !Objects.equals(existing.getId(), team.getId()))
                .anyMatch(existing -> existing.getJoinCode().equals(team.getJoinCode()));
        if (duplicateJoinCode) {
            throw new DuplicateJoinCodeException(team.getJoinCode());
        }

        if (team.getId() == null) {
            long nextId = teams.stream().mapToLong(Team::getId).max().orElse(0L) + 1;
            team.setId(nextId);
            teams.add(team);
        } else {
            int index = indexOfId(teams, team.getId());
            if (index < 0) {
                throw new IllegalStateException("Team not found: " + team.getId());
            }
            teams.set(index, team);
        }
        writeAll(teams);
        return team;
    }

    @Override
    public synchronized boolean existsByJoinCode(String joinCode) {
        return findByJoinCode(joinCode).isPresent();
    }

    @Override
    public synchronized Optional<Team> findByJoinCode(String joinCode) {
        return readAll().stream().filter(t -> t.getJoinCode().equals(joinCode)).findFirst();
    }

    private int indexOfId(List<Team> teams, long id) {
        for (int i = 0; i < teams.size(); i++) {
            if (teams.get(i).getId() == id) {
                return i;
            }
        }
        return -1;
    }

    private List<Team> readAll() {
        List<Team> teams = new ArrayList<>();
        if (!Files.exists(file)) {
            return teams;
        }
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (!line.isBlank()) {
                    teams.add(parse(line));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read teams from " + file, e);
        }
        return teams;
    }

    private void writeAll(List<Team> teams) {
        List<String> lines = teams.stream().map(this::format).toList();
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Files.write(file, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save teams to " + file, e);
        }
    }

    private String format(Team team) {
        String members = team.getMembers().stream()
                .map(m -> m.userId() + ":" + m.role())
                .collect(Collectors.joining(","));
        return String.join("|",
                String.valueOf(team.getId()),
                URLEncoder.encode(team.getName(), StandardCharsets.UTF_8),
                team.getJoinCode(),
                members);
    }

    private Team parse(String line) {
        String[] parts = line.split("\\|", -1);
        List<Team.Member> members = new ArrayList<>();
        if (!parts[3].isBlank()) {
            for (String entry : parts[3].split(",")) {
                String[] pair = entry.split(":");
                members.add(new Team.Member(Long.parseLong(pair[0]), Team.Role.valueOf(pair[1])));
            }
        }
        return Team.restore(Long.parseLong(parts[0]),
                URLDecoder.decode(parts[1], StandardCharsets.UTF_8), parts[2], members);
    }
}
