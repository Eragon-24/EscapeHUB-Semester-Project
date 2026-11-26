package com.escapehub.evidence.web;

import com.escapehub.evidence.application.JournalService;
import com.escapehub.evidence.domain.EvidenceEntry;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class JournalController {

    private final JournalService journalService;

    public JournalController(final JournalService journalService) {
        this.journalService = journalService;
    }

    @GetMapping("/game-sessions/{joinCode}/journal/view")
    public String showJournal(
            @PathVariable final String joinCode, final Principal principal, final Model model) {
        try {
            journalService.viewJournal(joinCode, principal.getName(), null);
        } catch (final SecurityException exception) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, exception.getMessage());
        } catch (final IllegalArgumentException exception) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.NOT_FOUND, exception.getMessage());
        }
        model.addAttribute("joinCode", joinCode.trim().toUpperCase(java.util.Locale.ROOT));
        return "journal";
    }

    @GetMapping("/game-sessions/{joinCode}/journal")
    @ResponseBody
    public ResponseEntity<?> getJournal(
            @PathVariable final String joinCode,
            @RequestParam(name = "q", required = false) final String query,
            final Principal principal) {
        try {
            final List<EvidenceEntry> entries =
                    journalService.viewJournal(joinCode, principal.getName(), query);
            return ResponseEntity.ok(entries);
        } catch (final SecurityException exception) {
            return error(HttpStatus.FORBIDDEN, exception.getMessage());
        } catch (final IllegalArgumentException exception) {
            return error(HttpStatus.NOT_FOUND, exception.getMessage());
        }
    }

    private ResponseEntity<?> error(final HttpStatus status, final String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}
