package com.escapehub.gameplay.domain;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExitAccessServiceTest {

    private final ExitAccessService service = new ExitAccessService();

    @Test
    void shouldAllowAccessWhenAllRequiredItemsAreAvailable() {
        final RoomExit exit = new RoomExit(
                "LAB_EXIT",
                "STORAGE_ROOM",
                Set.of(ItemType.LAB_KEY)
        );

        final ExitAccessResult result =
                service.checkAccess(exit, List.of(ItemType.LAB_KEY));

        assertTrue(result.allowed());
        assertTrue(result.missingItems().isEmpty());
    }

    @Test
    void shouldRejectAccessWhenRequiredKeyIsMissing() {
        final RoomExit exit = new RoomExit(
                "STORAGE_EXIT",
                "OFFICE_ROOM",
                Set.of(ItemType.STORAGE_KEY)
        );

        final ExitAccessResult result =
                service.checkAccess(exit, List.of(ItemType.LAB_KEY));

        assertFalse(result.allowed());
        assertEquals(Set.of(ItemType.STORAGE_KEY), result.missingItems());
    }

    @Test
    void shouldReportEveryMissingItemForMultiItemExit() {
        final RoomExit exit = new RoomExit(
                "OFFICE_EXIT",
                "CENTRAL_ROOM",
                Set.of(ItemType.OFFICE_KEY, ItemType.LAB_KEY)
        );

        final ExitAccessResult result =
                service.checkAccess(exit, List.of(ItemType.OFFICE_KEY));

        assertFalse(result.allowed());
        assertEquals(Set.of(ItemType.LAB_KEY), result.missingItems());
    }

    @Test
    void shouldAllowAccessWhenExitHasNoRequirements() {
        final RoomExit exit = new RoomExit("CORRIDOR_EXIT", "CORRIDOR");

        final ExitAccessResult result = service.checkAccess(exit, List.of());

        assertTrue(result.allowed());
        assertTrue(result.missingItems().isEmpty());
    }

    @Test
    void shouldKeepRequirementsIndependentBetweenExits() {
        final RoomExit labExit = new RoomExit(
                "LAB_EXIT",
                "STORAGE_ROOM",
                Set.of(ItemType.LAB_KEY)
        );
        final RoomExit storageExit = new RoomExit(
                "STORAGE_EXIT",
                "OFFICE_ROOM",
                Set.of(ItemType.STORAGE_KEY)
        );
        final List<ItemType> labItems = List.of(ItemType.LAB_KEY);

        assertTrue(service.checkAccess(labExit, labItems).allowed());
        assertFalse(service.checkAccess(storageExit, labItems).allowed());
    }

    @Test
    void shouldNotShareItemsAcrossGameSessions() {
        final RoomExit exit = new RoomExit(
                "LAB_EXIT",
                "STORAGE_ROOM",
                Set.of(ItemType.LAB_KEY)
        );
        final Set<ItemType> firstSessionItems = Set.of(ItemType.LAB_KEY);
        final Set<ItemType> secondSessionItems = Set.of();

        assertTrue(service.checkAccess(exit, firstSessionItems).allowed());
        assertFalse(service.checkAccess(exit, secondSessionItems).allowed());
    }

    @Test
    void shouldCreateImmutableExitAndResultSets() {
        final java.util.HashSet<ItemType> requirements = new java.util.HashSet<>();
        requirements.add(ItemType.LAB_KEY);
        final RoomExit exit =
                new RoomExit("LAB_EXIT", "STORAGE_ROOM", requirements);
        requirements.clear();

        assertEquals(Set.of(ItemType.LAB_KEY), exit.getRequiredItems());
        assertThrows(
                UnsupportedOperationException.class,
                () -> exit.getRequiredItems().clear()
        );
    }

    @Test
    void shouldRejectNullInputs() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.checkAccess(null, List.of())
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> service.checkAccess(
                        new RoomExit("FREE_EXIT", "CORRIDOR"),
                        null
                )
        );
    }
}
