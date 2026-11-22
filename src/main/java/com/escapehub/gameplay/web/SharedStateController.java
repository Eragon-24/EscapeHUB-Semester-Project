package com.escapehub.gameplay.web;

import com.escapehub.gameplay.application.SessionUpdateChannel;
import com.escapehub.gameplay.application.SharedStateService;
import com.escapehub.gameplay.application.StateChangeRejectedException;
import com.escapehub.gameplay.domain.SharedStateSnapshot;
import com.escapehub.gameplay.domain.SharedStateUpdate;
import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * US-015: real-time delivery of shared game-state updates inside one game session.
 *
 * <p>{@code GET /game-sessions/{id}/events} opens a Server-Sent Events stream for a participant of that session.
 * {@code POST /game-sessions/{id}/state} asks the server to change the shared state; the server validates the
 * request, applies it and publishes the update to the session's participants.</p>
 */
@RestController
public class SharedStateController {

    private static final long STREAM_TIMEOUT_MILLIS = 30L * 60 * 1000;

    private final SharedStateService service;
    private final SessionUpdateChannel channel;

    /**
     * Creates the controller.
     *
     * @param service the shared-state service
     * @param channel the real-time delivery channel
     */
    public SharedStateController(final SharedStateService service, final SessionUpdateChannel channel) {
        this.service = service;
        this.channel = channel;
    }

    /**
     * Opens the real-time stream of a session. The first event is a {@code snapshot} with the full current state,
     * followed by one {@code update} event per accepted change. Only participants of the session may subscribe.
     *
     * @param sessionId the session
     * @param principal the authenticated user
     * @return the event stream
     */
    @GetMapping(path = "/game-sessions/{sessionId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable final UUID sessionId, final Principal principal) {
        service.requireParticipant(sessionId, principal.getName());
        final SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MILLIS);
        final Runnable unsubscribe = channel.subscribe(sessionId, update -> emitter.send(
                SseEmitter.event()
                        .id(String.valueOf(update.version()))
                        .name("update")
                        .data(update, MediaType.APPLICATION_JSON)));
        emitter.onCompletion(unsubscribe);
        emitter.onTimeout(emitter::complete);
        emitter.onError(error -> unsubscribe.run());
        // The snapshot is taken after subscribing, so no update can be missed. A client ignores updates whose
        // version is not greater than the snapshot's.
        final SharedStateSnapshot snapshot = service.snapshot(sessionId, principal.getName());
        try {
            emitter.send(SseEmitter.event().name("snapshot").data(snapshot, MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            unsubscribe.run();
            emitter.completeWithError(e);
        }
        return emitter;
    }

    /**
     * Requests a change of the shared state.
     *
     * @param sessionId the session
     * @param key the state entry
     * @param value the requested value
     * @param principal the authenticated user
     * @return the accepted update
     */
    @PostMapping("/game-sessions/{sessionId}/state")
    public SharedStateUpdate change(
            @PathVariable final UUID sessionId,
            @RequestParam final String key,
            @RequestParam final String value,
            final Principal principal) {
        return service.applyChange(sessionId, principal.getName(), key, value);
    }

    /**
     * Maps an unknown session to 404.
     *
     * @param e the exception
     * @return the error body
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> notFound(final NoSuchElementException e) {
        return error(HttpStatus.NOT_FOUND, e.getMessage());
    }

    /**
     * Maps a refused change to 400.
     *
     * @param e the exception
     * @return the error body
     */
    @ExceptionHandler(StateChangeRejectedException.class)
    public ResponseEntity<Map<String, String>> rejected(final StateChangeRejectedException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /**
     * Maps a change to an inactive session to 409.
     *
     * @param e the exception
     * @return the error body
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> conflict(final IllegalStateException e) {
        return error(HttpStatus.CONFLICT, e.getMessage());
    }

    private ResponseEntity<Map<String, String>> error(final HttpStatus status, final String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}
