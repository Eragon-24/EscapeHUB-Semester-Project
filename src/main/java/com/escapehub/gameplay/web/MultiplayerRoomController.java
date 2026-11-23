package com.escapehub.gameplay.web;

import com.escapehub.gameplay.domain.Room;
import java.security.Principal;
import java.util.Locale;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/** Serves the multiplayer room layout and its read-only object-inspection resource. */
@Controller
public class MultiplayerRoomController {

    private static final String JOIN_CODE_PATTERN = "[A-Za-z0-9]{6}";

    private final Room room = new Room();

    @GetMapping("/multiplayer-room")
    public String showRoom(
            @RequestParam(name = "session", required = false) final String session,
            final Principal principal,
            final Model model) {
        if (session != null && session.trim().matches(JOIN_CODE_PATTERN)
                && isAuthenticated(principal)) {
            model.addAttribute("activeSessionCode", session.trim().toUpperCase(Locale.ROOT));
        }
        model.addAttribute("roomName", room.getName());
        model.addAttribute("roomDescription", room.getDescription());
        model.addAttribute(
                "inspectableObjects",
                room.getSelectableObjects().stream()
                        .filter(objectName -> room.inspect(objectName).isPresent())
                        .map(objectName -> new InspectableObject(objectName, objectName))
                        .toList()
        );
        model.addAttribute("canCreateSession", isAuthenticated(principal));
        return "multiplayer-room";
    }

    /**
     * Returns the existing inspection information for an object in the current room.
     *
     * @param objectName object selected in the room
     * @return the object's name and information, or not found when it is unavailable
     */
    @GetMapping("/api/room/objects/{objectName}/inspection")
    @ResponseBody
    public ResponseEntity<InspectionResult> inspectObject(
            @PathVariable final String objectName) {
        if (!room.getSelectableObjects().contains(objectName)) {
            return ResponseEntity.notFound().build();
        }

        final Optional<String> inspection = room.inspect(objectName);
        return inspection
                .map(description -> ResponseEntity.ok(
                        new InspectionResult(objectName, description)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private boolean isAuthenticated(final Principal principal) {
        return principal != null && !"anonymousUser".equals(principal.getName());
    }

    /** Object name and identifier rendered in the room interface. */
    public record InspectableObject(String id, String name) { }

    /** Informational response returned by the room inspection endpoint. */
    public record InspectionResult(String name, String description) { }
}
