package com.escapehub.gameplay.application;

import com.escapehub.gameplay.domain.ExitAccessResult;
import com.escapehub.gameplay.domain.ExitAccessService;
import com.escapehub.gameplay.domain.ItemType;
import com.escapehub.gameplay.domain.Room;
import com.escapehub.gameplay.domain.RoomExit;
import com.escapehub.puzzle.domain.AnswerResult;
import com.escapehub.puzzle.domain.ChestPuzzle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Holds the authoritative in-memory state for the shared LAN demonstration.
 * Each connected browser has its own player position, while discoveries,
 * puzzle progress and the exit state belong to the shared game session.
 */
@Service
public class GameSessionService {

    private static final int MAX_PLAYERS = 8;
    private static final double PLAYER_RADIUS = 15.0;
    private static final double MOVE_STEP = 9.0;
    private static final double INTERACTION_RANGE = 95.0;

    private static final int ROOM_LEFT = 75;
    private static final int ROOM_RIGHT = 885;
    private static final int ROOM_TOP = 145;
    private static final int ROOM_BOTTOM = 550;

    private static final Rectangle2D DESK = new Rectangle2D.Double(150, 180, 270, 120);
    private static final Rectangle2D CHEST = new Rectangle2D.Double(680, 195, 140, 110);
    private static final Rectangle2D EXIT_DOOR = new Rectangle2D.Double(430, 92, 100, 50);

    private static final Target[] TARGETS = {
        new Target(290, 230, "NOTEBOOK"),
        new Target(750, 250, "CHEST"),
        new Target(480, 117, "EXIT")
    };

    private final Map<String, PlayerState> players = new LinkedHashMap<>();
    private final Room room = new Room();
    private final ExitAccessService exitAccessService = new ExitAccessService();
    private final RoomExit demoExit = new RoomExit(
            "LAB_EXIT",
            "STORAGE_ROOM",
            Set.of(ItemType.LAB_KEY)
    );
    private final Set<ItemType> sharedItems = new java.util.HashSet<>();

    private ChestPuzzle chestPuzzle = new ChestPuzzle("483920");
    private boolean notebookFound;
    private boolean gameWon;
    private long nextPlayerNumber = 1;
    private String statusMessage = "Εξερεύνησε το εργαστήριο και βρες ένα στοιχείο.";

    /**
     * Adds a browser connection as a new player in the shared room.
     *
     * @return the unique player identifier
     */
    public synchronized String addPlayer() {
        if (players.size() >= MAX_PLAYERS) {
            throw new IllegalStateException("The demo room is full.");
        }

        final String playerId = UUID.randomUUID().toString();
        final long playerNumber = nextPlayerNumber++;
        final double spawnX = 360 + ((playerNumber - 1) % MAX_PLAYERS) * 35;
        players.put(playerId, new PlayerState(
                playerId,
                "Player " + playerNumber,
                spawnX,
                490
        ));
        statusMessage = "Ένας παίκτης συνδέθηκε στο εργαστήριο.";
        return playerId;
    }

    /**
     * Removes a disconnected player. The shared game resets when the last
     * browser leaves, so the next group starts a fresh demo.
     *
     * @param playerId disconnected player's identifier
     */
    public synchronized void removePlayer(final String playerId) {
        players.remove(playerId);
        if (players.isEmpty()) {
            resetSession();
        } else {
            statusMessage = "Ένας παίκτης αποσυνδέθηκε.";
        }
    }

    /**
     * Applies one authoritative movement step for a player.
     *
     * @param playerId player identifier
     * @param horizontal horizontal direction (-1, 0 or 1)
     * @param vertical vertical direction (-1, 0 or 1)
     * @return the current shared game snapshot for that player
     */
    public synchronized GameSnapshot movePlayer(
            final String playerId,
            final int horizontal,
            final int vertical) {
        if (horizontal < -1 || horizontal > 1 || vertical < -1 || vertical > 1
                || (horizontal == 0 && vertical == 0)) {
            throw new IllegalArgumentException("Movement direction must be -1, 0 or 1.");
        }

        final PlayerState player = requirePlayer(playerId);
        if (gameWon) {
            return snapshotFor(playerId);
        }

        final double length = Math.hypot(horizontal, vertical);
        final double deltaX = horizontal / length * MOVE_STEP;
        final double deltaY = vertical / length * MOVE_STEP;
        moveAxis(player, player.x + deltaX, player.y, true);
        moveAxis(player, player.x, player.y + deltaY, false);
        return snapshotFor(playerId);
    }

    /**
     * Interacts with the nearest object if the player is close enough.
     *
     * @param playerId player identifier
     * @return the current shared game snapshot
     */
    public synchronized GameSnapshot interact(final String playerId) {
        final PlayerState player = requirePlayer(playerId);
        final Target target = findNearbyTarget(player);
        if (target == null) {
            statusMessage = "Δεν υπάρχει αντικείμενο αρκετά κοντά. Πλησίασε πρώτα.";
            return snapshotFor(playerId);
        }

        switch (target.name) {
            case "NOTEBOOK" -> {
                notebookFound = true;
                statusMessage = "Βρήκες το σημειωματάριο! Η ένδειξη είναι πλέον διαθέσιμη σε όλους.";
            }
            case "CHEST" -> {
                if (!notebookFound) {
                    statusMessage = "Το σεντούκι είναι κλειδωμένο. Ψάξε πρώτα το σημειωματάριο.";
                } else if (chestPuzzle.isSolved()) {
                    statusMessage = "Το σεντούκι είναι ήδη ανοιχτό. Η ομάδα έχει το κλειδί.";
                } else {
                    statusMessage = "Το σεντούκι περιμένει τον εξάψηφο κωδικό.";
                }
            }
            case "EXIT" -> unlockExit();
            default -> statusMessage = "Δεν υπάρχει διαθέσιμη ενέργεια.";
        }
        return snapshotFor(playerId);
    }

    /**
     * Checks a chest answer on the server. The player must be close to the
     * chest and the shared notebook clue must already have been discovered.
     *
     * @param playerId player identifier
     * @param answer submitted code
     * @return the current shared game snapshot
     */
    public synchronized GameSnapshot submitAnswer(
            final String playerId,
            final String answer) {
        final PlayerState player = requirePlayer(playerId);
        if (findNearbyTarget(player) == null
                || !"CHEST".equals(findNearbyTarget(player).name)) {
            statusMessage = "Πλησίασε πρώτα στο σεντούκι.";
            return snapshotFor(playerId);
        }
        if (!notebookFound) {
            statusMessage = "Πρέπει πρώτα να εξετάσεις το σημειωματάριο.";
            return snapshotFor(playerId);
        }
        if (chestPuzzle.isSolved()) {
            statusMessage = "Το σεντούκι είναι ήδη ανοιχτό.";
            return snapshotFor(playerId);
        }
        if (answer == null || answer.length() > 32) {
            statusMessage = AnswerResult.FAILURE_MESSAGE;
            return snapshotFor(playerId);
        }

        final AnswerResult result = chestPuzzle.check(answer);
        statusMessage = result.message();
        if (result.correct()) {
            sharedItems.add(ItemType.LAB_KEY);
            statusMessage += " Το σεντούκι άνοιξε! Η ομάδα πήρε το κλειδί.";
        }
        return snapshotFor(playerId);
    }

    /**
     * Returns an immutable view of the shared state for a connected player.
     *
     * @param playerId player identifier
     * @return current shared game state
     */
    public synchronized GameSnapshot snapshotFor(final String playerId) {
        requirePlayer(playerId);
        final List<PlayerSnapshot> playerSnapshots = new ArrayList<>();
        for (PlayerState player : players.values()) {
            playerSnapshots.add(new PlayerSnapshot(
                    player.id,
                    player.name,
                    player.x,
                    player.y
            ));
        }
        return new GameSnapshot(
                playerId,
                playerSnapshots,
                notebookFound,
                chestPuzzle.isSolved(),
                gameWon,
                statusMessage,
                notebookFound ? Room.NOTEBOOK_CLUE : ""
        );
    }

    private void unlockExit() {
        final ExitAccessResult access = exitAccessService.checkAccess(demoExit, sharedItems);
        if (!access.allowed()) {
            statusMessage = "Η πόρτα είναι κλειδωμένη. Χρειάζεται το κλειδί από το σεντούκι.";
            return;
        }
        gameWon = true;
        statusMessage = "Η έξοδος ξεκλειδώθηκε. Η ομάδα ολοκλήρωσε την αποστολή!";
    }

    private void moveAxis(
            final PlayerState player,
            final double nextX,
            final double nextY,
            final boolean horizontal) {
        if (canMoveTo(nextX, nextY)) {
            if (horizontal) {
                player.x = nextX;
            } else {
                player.y = nextY;
            }
        }
    }

    private boolean canMoveTo(final double x, final double y) {
        if (x < ROOM_LEFT + PLAYER_RADIUS || x > ROOM_RIGHT - PLAYER_RADIUS
                || y < ROOM_TOP || y > ROOM_BOTTOM - PLAYER_RADIUS) {
            return false;
        }

        final Rectangle2D playerBounds = new Rectangle2D.Double(
                x - PLAYER_RADIUS,
                y - PLAYER_RADIUS,
                PLAYER_RADIUS * 2,
                PLAYER_RADIUS * 2
        );
        return !playerBounds.intersects(DESK)
                && !playerBounds.intersects(CHEST)
                && !playerBounds.intersects(EXIT_DOOR);
    }

    private Target findNearbyTarget(final PlayerState player) {
        Target nearest = null;
        double nearestDistance = INTERACTION_RANGE;
        for (Target target : TARGETS) {
            final double distance = Math.hypot(player.x - target.x, player.y - target.y);
            if (distance <= nearestDistance) {
                nearest = target;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private PlayerState requirePlayer(final String playerId) {
        final PlayerState player = players.get(playerId);
        if (player == null) {
            throw new IllegalArgumentException("Unknown player.");
        }
        return player;
    }

    private void resetSession() {
        notebookFound = false;
        gameWon = false;
        sharedItems.clear();
        chestPuzzle = new ChestPuzzle("483920");
        nextPlayerNumber = 1;
        statusMessage = "Εξερεύνησε το εργαστήριο και βρες ένα στοιχείο.";
    }

    /** Immutable state sent to each browser. */
    public record GameSnapshot(
            String you,
            List<PlayerSnapshot> players,
            boolean notebookFound,
            boolean chestSolved,
            boolean gameWon,
            String statusMessage,
            String clueText) {
        public GameSnapshot {
            players = List.copyOf(players);
        }
    }

    /** Immutable player position sent to browsers. */
    public record PlayerSnapshot(String id, String name, double x, double y) { }

    private static final class PlayerState {
        private final String id;
        private final String name;
        private double x;
        private double y;

        private PlayerState(
                final String id,
                final String name,
                final double x,
                final double y) {
            this.id = id;
            this.name = name;
            this.x = x;
            this.y = y;
        }
    }

    private record Target(double x, double y, String name) { }
}
