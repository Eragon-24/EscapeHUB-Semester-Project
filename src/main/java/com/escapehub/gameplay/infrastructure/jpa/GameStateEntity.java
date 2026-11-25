package com.escapehub.gameplay.infrastructure.jpa;

import com.escapehub.gameplay.domain.GameState;
import com.escapehub.gameplay.domain.GameStatus;
import com.escapehub.gameplay.domain.ItemType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "game_states")
public class GameStateEntity {

    @Id
    @Column(name = "session_id", nullable = false, updatable = false)
    private UUID sessionId;

    @Column(name = "current_room_id", nullable = false, length = 120)
    private String currentRoomId;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_status", nullable = false, length = 32)
    private GameStatus status;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "game_state_attributes",
            joinColumns = @JoinColumn(name = "session_id"))
    @MapKeyColumn(name = "attribute_key", length = 64)
    @Column(name = "attribute_value", nullable = false, length = 1000)
    private Map<String, String> attributes = new HashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "game_state_completed_puzzles",
            joinColumns = @JoinColumn(name = "session_id"))
    @Column(name = "puzzle_id", nullable = false, length = 120)
    private Set<String> completedPuzzleIds = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "game_state_collected_items",
            joinColumns = @JoinColumn(name = "session_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 50)
    private Set<ItemType> collectedItems = new HashSet<>();

    @Column(name = "exit_unlocked", nullable = false)
    private boolean exitUnlocked;

    @Column(name = "state_version", nullable = false)
    private long version;

    protected GameStateEntity() {
    }

    public GameStateEntity(final GameState state) {
        updateFrom(state);
    }

    public void updateFrom(final GameState state) {
        sessionId = state.sessionId();
        currentRoomId = state.currentRoomId();
        status = state.status();
        attributes.clear();
        attributes.putAll(state.attributes());
        completedPuzzleIds.clear();
        completedPuzzleIds.addAll(state.completedPuzzleIds());
        collectedItems.clear();
        collectedItems.addAll(state.collectedItems());
        exitUnlocked = state.exitUnlocked();
        version = state.version();
    }

    public GameState toDomain() {
        return new GameState(
                sessionId,
                currentRoomId,
                status,
                attributes,
                completedPuzzleIds,
                collectedItems,
                exitUnlocked,
                version);
    }
}
