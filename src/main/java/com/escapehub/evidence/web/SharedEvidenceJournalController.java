package com.escapehub.evidence.web;

import com.escapehub.evidence.application.AddEvidenceCommand;
import com.escapehub.evidence.application.SharedEvidenceJournalService;
import com.escapehub.evidence.domain.EvidenceEntry;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for US-020 adding discovered evidence to a session's shared journal
 * and retrieving the shared journal entries.
 */
@RestController
public class SharedEvidenceJournalController {

    private final SharedEvidenceJournalService journalService;

    /**
     * Creates the REST controller for session shared evidence journals.
     *
     * @param journalService application service managing shared journals
     */
    public SharedEvidenceJournalController(final SharedEvidenceJournalService journalService) {
        this.journalService = journalService;
    }

    /**
     * Adds an eligible discovered clue to the current session's shared journal.
     *
     * @param sessionId unique session identifier
     * @param request evidence payload
     * @param principal authenticated user
     * @return 201 Created with the added entry, or an appropriate error status
     */
    @PostMapping("/api/v1/game-sessions/{sessionId}/journal")
    public ResponseEntity<?> addEvidence(
            @PathVariable("sessionId") final UUID sessionId,
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
                    sessionId,
                    principal.getName(),
                    new AddEvidenceCommand(
                            request.evidenceId(),
                            request.title(),
                            request.description(),
                            request.source()
                    )
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (final RuntimeException exception) {
            return mapException(exception);
        }
    }

    /**
     * Returns all evidence entries currently stored in the session's shared journal.
     *
     * @param sessionId unique session identifier
     * @param principal authenticated user
     * @return 200 OK with shared journal entries, or an appropriate error status
     */
    @GetMapping("/api/v1/game-sessions/{sessionId}/journal")
    public ResponseEntity<?> getSharedJournal(
            @PathVariable("sessionId") final UUID sessionId,
            final Principal principal) {
        if (!isAuthenticated(principal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            final List<EvidenceEntry> entries = journalService.getJournalEntries(
                    sessionId, principal.getName());
            return ResponseEntity.ok(entries);
        } catch (final RuntimeException exception) {
            return mapException(exception);
        }
    }

    private ResponseEntity<?> mapException(final RuntimeException exception) {
        if (exception instanceof NoSuchElementException) {
            return error(HttpStatus.NOT_FOUND, exception.getMessage());
        }
        if (exception instanceof SecurityException) {
            return error(HttpStatus.FORBIDDEN, exception.getMessage());
        }
        if (exception instanceof IllegalStateException) {
            return error(HttpStatus.CONFLICT, exception.getMessage());
        }
        if (exception instanceof IllegalArgumentException) {
            return error(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
        throw exception;
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
     * @param evidenceId eligible clue identifier
     * @param title optional custom display title
     * @param description optional custom description
     * @param source optional custom source label
     */
    public record AddEvidenceRequest(
            String evidenceId,
            String title,
            String description,
            String source) {
    }
}