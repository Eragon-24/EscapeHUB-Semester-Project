package com.escapehub.puzzle.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.escapehub.gameplay.application.GameSessionNotFoundException;
import com.escapehub.puzzle.application.CommonPuzzleProgressService;
import com.escapehub.puzzle.domain.CommonPuzzleProgressView;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class CommonPuzzleProgressControllerTest {
    @Mock
    private CommonPuzzleProgressService service;

    private CommonPuzzleProgressController controller;
    private UUID sessionId;
    private Principal alice;

    @BeforeEach
    void setUp() {
        controller = new CommonPuzzleProgressController(service);
        sessionId = UUID.randomUUID();
        alice = () -> "alice";
    }

    @Test
    void returnsProgressForAuthorizedParticipant() {
        CommonPuzzleProgressView view = new CommonPuzzleProgressView(
                sessionId, "laboratory", 1, 0, 1, 0, 0, List.of());
        when(service.getCommonProgress(sessionId, "alice")).thenReturn(view);

        ResponseEntity<?> response = controller.getProgress(sessionId, alice);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(view, response.getBody());
    }

    @Test
    void rejectsMissingOrAnonymousPrincipal() {
        assertEquals(HttpStatus.UNAUTHORIZED, controller.getProgress(sessionId, null).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED,
                controller.getProgress(sessionId, () -> "anonymousUser").getStatusCode());
    }

    @Test
    void mapsForbiddenError() {
        when(service.getCommonProgress(sessionId, "alice"))
                .thenThrow(new SecurityException("forbidden"));

        assertEquals(HttpStatus.FORBIDDEN, controller.getProgress(sessionId, alice).getStatusCode());
    }

    @Test
    void mapsNotFoundError() {
        when(service.getCommonProgress(sessionId, "alice"))
                .thenThrow(new GameSessionNotFoundException("missing"));

        assertEquals(HttpStatus.NOT_FOUND, controller.getProgress(sessionId, alice).getStatusCode());
    }

    @Test
    void mapsInactiveSessionError() {
        when(service.getCommonProgress(sessionId, "alice"))
                .thenThrow(new IllegalStateException("inactive"));

        assertEquals(HttpStatus.CONFLICT, controller.getProgress(sessionId, alice).getStatusCode());
    }
}
