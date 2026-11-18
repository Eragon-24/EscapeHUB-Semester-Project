package com.escapehub.gameplay.domain;

import java.util.Set;

/**
 * Describes an exit and the items required to use it.
 */
public class RoomExit {

    private final String identifier;
    private final String destinationRoom;
    private final Set<ItemType> requiredItems;

    /**
     * Creates an exit with no item requirements.
     *
     * @param identifier unique exit identifier
     * @param destinationRoom room reached through the exit
     */
    public RoomExit(final String identifier, final String destinationRoom) {
        this(identifier, destinationRoom, Set.of());
    }

    /**
     * Creates an exit with item requirements.
     *
     * @param identifier unique exit identifier
     * @param destinationRoom room reached through the exit
     * @param requiredItems items required for access
     */
    public RoomExit(
            final String identifier,
            final String destinationRoom,
            final Set<ItemType> requiredItems) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Exit identifier must not be blank");
        }
        if (destinationRoom == null || destinationRoom.isBlank()) {
            throw new IllegalArgumentException("Destination room must not be blank");
        }
        if (requiredItems == null) {
            throw new IllegalArgumentException("Required items must not be null");
        }

        this.identifier = identifier;
        this.destinationRoom = destinationRoom;
        this.requiredItems = Set.copyOf(requiredItems);
    }

    /**
     * @return the unique exit identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * @return the destination room identifier
     */
    public String getDestinationRoom() {
        return destinationRoom;
    }

    /**
     * @return immutable set of required items
     */
    public Set<ItemType> getRequiredItems() {
        return requiredItems;
    }
}
