package com.escapehub.gameplay.web;

import com.escapehub.gameplay.application.MissionRunOverview;
import com.escapehub.gameplay.application.MissionRunService;
import com.escapehub.gameplay.domain.MissionRun;
import java.security.Principal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * US-015: σελίδα με τις αποστολές του συνδεδεμένου παίκτη, χωρισμένες σε ενεργές και ολοκληρωμένες.
 */
@Controller
public class MissionRunController {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final MissionRunService service;
    private final ZoneId zone;

    /**
     * Δημιουργεί τον controller.
     *
     * @param service η υπηρεσία εκτελέσεων
     * @param clock το ρολόι (δίνει τη ζώνη ώρας εμφάνισης)
     */
    public MissionRunController(final MissionRunService service, final Clock clock) {
        this.service = service;
        this.zone = clock.getZone();
    }

    /**
     * Εμφανίζει τις ενεργές και ολοκληρωμένες αποστολές του παίκτη. Απαιτεί σύνδεση.
     *
     * @param principal ο συνδεδεμένος χρήστης
     * @param model το μοντέλο
     * @return το template της σελίδας
     */
    @GetMapping("/my-missions")
    public String myMissions(final Principal principal, final Model model) {
        final MissionRunOverview overview = service.overviewFor(principal.getName());
        model.addAttribute("username", principal.getName());
        model.addAttribute("activeRuns", rows(overview.active()));
        model.addAttribute("completedRuns", rows(overview.completed()));
        return "my-missions";
    }

    private List<RunRow> rows(final List<MissionRun> runs) {
        return runs.stream().map(this::toRow).toList();
    }

    private RunRow toRow(final MissionRun run) {
        return new RunRow(
                run.missionTitle(),
                run.status().getLabel(),
                format(run.startedAt()),
                run.completedAt() == null ? null : format(run.completedAt()));
    }

    private String format(final Instant instant) {
        return FORMAT.format(instant.atZone(zone));
    }

    /**
     * Γραμμή προς εμφάνιση στη λίστα.
     *
     * @param missionTitle ο τίτλος της αποστολής
     * @param statusLabel η ετικέτα κατάστασης
     * @param startedAt η ώρα έναρξης (μορφοποιημένη)
     * @param completedAt η ώρα ολοκλήρωσης (μορφοποιημένη) ή null
     */
    public record RunRow(String missionTitle, String statusLabel, String startedAt, String completedAt) {
    }
}
