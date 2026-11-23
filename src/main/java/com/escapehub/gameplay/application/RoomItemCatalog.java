package com.escapehub.gameplay.application;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Configurable list of collectible items available in a room. */
@Component
public class RoomItemCatalog {

    private static final Pattern ITEM_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    private final Set<String> laboratoryItems;

    public RoomItemCatalog(@Value("${escapehub.game-items.abandoned-laboratory:lab-notebook}")
                           final List<String> laboratoryItems) {
        if (laboratoryItems == null || laboratoryItems.isEmpty()
                || laboratoryItems.stream().anyMatch(item -> item == null || !ITEM_ID.matcher(item).matches())) {
            throw new IllegalArgumentException("Room item IDs must be valid and at least one item must be configured");
        }
        this.laboratoryItems = Set.copyOf(laboratoryItems);
    }

    public List<String> itemsIn(final String roomId) {
        return "abandoned-laboratory".equals(roomId) ? laboratoryItems.stream().sorted().toList() : List.of();
    }

    public boolean contains(final String roomId, final String itemId) {
        return itemsIn(roomId).contains(itemId);
    }
}
