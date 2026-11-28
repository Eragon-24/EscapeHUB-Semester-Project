package com.escapehub.evidence.web;

import com.escapehub.evidence.application.AddEvidenceCommand;
import com.escapehub.evidence.application.DuplicateEvidenceException;
import com.escapehub.evidence.application.InactiveSessionException;
import com.escapehub.evidence.application.InvalidEvidenceException;
import com.escapehub.evidence.application.JournalService;
import com.escapehub.evidence.application.SessionNotFoundException;
import com.escapehub.evidence.domain.EvidenceEntry;
import java.security.Principal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

/**
 * Web and REST endpoints for viewing, searching, and adding evidence to a session's
 * shared journal.
 */
@Controller
public class JournalController {

    private final JournalService journalService;

    /**
     * Creates the journal controller backed by the unified {@link JournalService}.
     *
     * @param journalService application service managing the shared evidence journal
     */
    public JournalController(final JournalService journalService) {
        this.journalService = journalService;
    }

    /**
     * Renders the shared journal HTML view for an authorized session participant.
     *
     * @param joinCode  session join code
     * @param principal authenticated user
     * @param model     view model
     * @return template name
     */
    @GetMapping("/game-sessions/{joinCode}/journal/view")
    public String showJournal(
            @PathVariable final String joinCode, final Principal principal, final Model model) {
        try {
            journalService.viewJournal(joinCode, principal.getName(), null);
        } catch (final SecurityException exception) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, exception.getMessage());
        } catch (final InactiveSessionException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
        } catch (final SessionNotFoundException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage());
        } catch (final InvalidEvidenceException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
        model.addAttribute("joinCode", joinCode.trim().toUpperCase(Locale.ROOT));
        return "journal";
    }

    /**
     * Returns shared journal entries for the session, optionally filtered by query.
     *
     * @param joinCode  session join code
     * @param query     optional search term
     * @param principal authenticated user
     * @return 200 OK with matching entries, or an error status
     */
    @GetMapping("/game-sessions/{joinCode}/journal")
    @ResponseBody
    public ResponseEntity<?> getJournal(
            @PathVariable final String joinCode,
            @RequestParam(name = "q", required = false) final String query,
            final Principal principal) {
        if (!isAuthenticated(principal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            final List<EvidenceEntry> entries =
                    journalService.viewJournal(joinCode, principal.getName(), query);
            return ResponseEntity.ok(entries);
        } catch (final SecurityException exception) {
            return error(HttpStatus.FORBIDDEN, exception.getMessage());
        } catch (final InactiveSessionException exception) {
            return error(HttpStatus.CONFLICT, exception.getMessage());
        } catch (final SessionNotFoundException exception) {
            return error(HttpStatus.NOT_FOUND, exception.getMessage());
        } catch (final InvalidEvidenceException exception) {
            return error(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }

    /**
     * US-020: Adds an eligible discovered clue to the current session's shared journal.
     *
     * @param joinCode  session join code
     * @param request   payload identifying the discovered clue
     * @param principal authenticated user
     * @return 201 Created with the added entry, or an appropriate error status
     */
    @PostMapping("/game-sessions/{joinCode}/journal")
    @ResponseBody
    public ResponseEntity<?> addEvidence(
            @PathVariable final String joinCode,
            @RequestBody(required = false) final AddEvidenceRequest request,
            final Principal principal) {
        if (!isAuthenticated(principal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (request == null || request.evidenceId() == null || request.evidenceId().isBlank()) {
            return error(HttpStatus.BAD_REQUEST, "Το αναγνωριστικό στοιχείου είναι υποχρεωτικό.");
        }
        try {
            final EvidenceEntry created = journalService.addEvidence(
                    joinCode,
                    principal.getName(),
                    new AddEvidenceCommand(
                            request.evidenceId(),
                            request.title(),
                            request.description(),
                            request.source()
                    )
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (final SecurityException exception) {
            return error(HttpStatus.FORBIDDEN, exception.getMessage());
        } catch (final DuplicateEvidenceException | InactiveSessionException exception) {
            return error(HttpStatus.CONFLICT, exception.getMessage());
        } catch (final SessionNotFoundException exception) {
            return error(HttpStatus.NOT_FOUND, exception.getMessage());
        } catch (final InvalidEvidenceException exception) {
            return error(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }

    private ResponseEntity<?> error(
            final HttpStatus status, final String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }

    private boolean isAuthenticated(final Principal principal) {
        return principal != null && !"anonymousUser".equals(principal.getName());
    }

    /**
     * Request payload for adding a discovered clue to the shared journal.
     *
     * @param evidenceId  eligible clue identifier
     * @param title       optional custom display title
     * @param description optional custom description
     * @param source      optional custom source label
     */
    public record AddEvidenceRequest(
            String evidenceId,
            String title,
            String description,
            String source) {
    }
}