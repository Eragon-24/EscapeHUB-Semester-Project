package com.escapehub.gameplay.ui;

import com.escapehub.gameplay.domain.Room;
import com.escapehub.gameplay.domain.ExitAccessResult;
import com.escapehub.gameplay.domain.ExitAccessService;
import com.escapehub.gameplay.domain.ItemType;
import com.escapehub.gameplay.domain.RoomExit;
import com.escapehub.puzzle.domain.AnswerResult;
import com.escapehub.puzzle.domain.ChestPuzzle;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * US-006: Απλό 2D top-down demo του εγκαταλελειμμένου εργαστηρίου.
 * Ο παίκτης κινείται, εξετάζει το σημειωματάριο, ανοίγει το
 * σεντούκι και ξεκλειδώνει την έξοδο.
 */
public class TopDownRoomView {

    private static final int WINDOW_WIDTH = 960;
    private static final int WINDOW_HEIGHT = 640;
    private static final int PLAYER_RADIUS = 15;
    private static final int MOVE_SPEED = 3;
    private static final int INTERACTION_RANGE = 95;
    private static final int TIMER_DELAY_MS = 16;
    private static final String CHEST_CODE = "483920";

    private static final int ROOM_LEFT = 75;
    private static final int ROOM_RIGHT = 885;
    private static final int ROOM_TOP = 145;
    private static final int ROOM_BOTTOM = 550;

    private static final Rectangle2D DESK = new Rectangle2D.Double(150, 180, 270, 120);
    private static final Rectangle2D CHEST = new Rectangle2D.Double(680, 195, 140, 110);
    private static final Rectangle2D EXIT_DOOR = new Rectangle2D.Double(430, 92, 100, 50);

    private static final double NOTEBOOK_X = 290;
    private static final double NOTEBOOK_Y = 230;
    private static final double CHEST_X = 750;
    private static final double CHEST_Y = 250;
    private static final double DOOR_X = 480;
    private static final double DOOR_Y = 117;

    private final Room room;
    private final ChestPuzzle chestPuzzle;
    private final Set<Integer> pressedKeys = new HashSet<>();
    private final Set<ItemType> sessionItems = new HashSet<>();
    private final JPanel gamePanel;
    private final ExitAccessService exitAccessService = new ExitAccessService();
    private final RoomExit demoExit = new RoomExit(
            "LAB_EXIT",
            "STORAGE_ROOM",
            Set.of(ItemType.LAB_KEY)
    );

    private JFrame frame;
    private Timer gameTimer;
    private double playerX = 480;
    private double playerY = 490;
    private boolean interactionKeyHeld;
    private boolean clueFound;
    private boolean hasKey;
    private boolean gameWon;
    private String statusMessage = "Εξερεύνησε το εργαστήριο και βρες "
            + "ένα στοιχείο.";

    /**
     * Δημιουργεί το demo χρησιμοποιώντας το υπάρχον μοντέλο Room.
     *
     * @param room το δωμάτιο που θα παρουσιαστεί
     */
    public TopDownRoomView(final Room room) {
        this.room = room;
        this.chestPuzzle = new ChestPuzzle(CHEST_CODE);
        this.gamePanel = new GamePanel();
        this.gamePanel.setPreferredSize(new java.awt.Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        this.gamePanel.setBorder(BorderFactory.createEmptyBorder());
        configureControls();
    }

    /** Εμφανίζει το παράθυρο του top-down demo. */
    public void showWindow() {
        frame = new JFrame("EscapeHub Demo - " + room.getName());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setContentPane(gamePanel);
        frame.pack();
        frame.setLocationRelativeTo(null);

        gameTimer = new Timer(TIMER_DELAY_MS, event -> updateGame());
        frame.setVisible(true);
        gamePanel.requestFocusInWindow();
        gameTimer.start();
    }

    private void configureControls() {
        bindKey("pressed W", "wDown", () -> pressedKeys.add(KeyEvent.VK_W));
        bindKey("released W", "wUp", () -> pressedKeys.remove(KeyEvent.VK_W));
        bindKey("pressed A", "aDown", () -> pressedKeys.add(KeyEvent.VK_A));
        bindKey("released A", "aUp", () -> pressedKeys.remove(KeyEvent.VK_A));
        bindKey("pressed S", "sDown", () -> pressedKeys.add(KeyEvent.VK_S));
        bindKey("released S", "sUp", () -> pressedKeys.remove(KeyEvent.VK_S));
        bindKey("pressed D", "dDown", () -> pressedKeys.add(KeyEvent.VK_D));
        bindKey("released D", "dUp", () -> pressedKeys.remove(KeyEvent.VK_D));

        bindKey("pressed UP", "upDown", () -> pressedKeys.add(KeyEvent.VK_UP));
        bindKey("released UP", "upUp", () -> pressedKeys.remove(KeyEvent.VK_UP));
        bindKey("pressed LEFT", "leftDown", () -> pressedKeys.add(KeyEvent.VK_LEFT));
        bindKey("released LEFT", "leftUp", () -> pressedKeys.remove(KeyEvent.VK_LEFT));
        bindKey("pressed DOWN", "downDown", () -> pressedKeys.add(KeyEvent.VK_DOWN));
        bindKey("released DOWN", "downUp", () -> pressedKeys.remove(KeyEvent.VK_DOWN));
        bindKey("pressed RIGHT", "rightDown", () -> pressedKeys.add(KeyEvent.VK_RIGHT));
        bindKey("released RIGHT", "rightUp", () -> pressedKeys.remove(KeyEvent.VK_RIGHT));

        bindKey("pressed E", "interactDown", () -> {
            if (!interactionKeyHeld) {
                interactionKeyHeld = true;
                interactWithNearbyObject();
            }
        });
        bindKey("released E", "interactUp", () -> interactionKeyHeld = false);
    }

    private void bindKey(
            final String keyStroke,
            final String actionName,
            final Runnable action) {
        gamePanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(keyStroke), actionName);
        gamePanel.getActionMap().put(actionName, new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent event) {
                action.run();
            }
        });
    }

    private void updateGame() {
        if (gameWon) {
            return;
        }

        int horizontal = 0;
        int vertical = 0;
        if (pressedKeys.contains(KeyEvent.VK_A) || pressedKeys.contains(KeyEvent.VK_LEFT)) {
            horizontal--;
        }
        if (pressedKeys.contains(KeyEvent.VK_D) || pressedKeys.contains(KeyEvent.VK_RIGHT)) {
            horizontal++;
        }
        if (pressedKeys.contains(KeyEvent.VK_W) || pressedKeys.contains(KeyEvent.VK_UP)) {
            vertical--;
        }
        if (pressedKeys.contains(KeyEvent.VK_S) || pressedKeys.contains(KeyEvent.VK_DOWN)) {
            vertical++;
        }

        if (horizontal != 0 || vertical != 0) {
            final double length = Math.sqrt(horizontal * horizontal + vertical * vertical);
            final double deltaX = horizontal / length * MOVE_SPEED;
            final double deltaY = vertical / length * MOVE_SPEED;
            movePlayer(deltaX, deltaY);
        }
        gamePanel.repaint();
    }

    private void movePlayer(final double deltaX, final double deltaY) {
        if (canMoveTo(playerX + deltaX, playerY)) {
            playerX += deltaX;
        }
        if (canMoveTo(playerX, playerY + deltaY)) {
            playerY += deltaY;
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

    private void interactWithNearbyObject() {
        final Target target = findNearbyTarget();
        if (target == null) {
            statusMessage = "Δεν υπάρχει αντικείμενο αρκετά κοντά. "
                    + "Πλησίασε πρώτα.";
            gamePanel.repaint();
            return;
        }

        switch (target) {
            case NOTEBOOK -> inspectNotebook();
            case CHEST -> inspectChest();
            case EXIT -> inspectExit();
            default -> statusMessage = "Δεν υπάρχει διαθέσιμη ενέργεια.";
        }
        gamePanel.repaint();
    }

    private Target findNearbyTarget() {
        Target nearest = null;
        double nearestDistance = INTERACTION_RANGE;
        final Target[] targets = Target.values();
        for (final Target target : targets) {
            final double distance = distanceTo(target.getX(), target.getY());
            if (distance <= nearestDistance) {
                nearest = target;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private double distanceTo(final double x, final double y) {
        return Math.hypot(playerX - x, playerY - y);
    }

    private void inspectNotebook() {
        final Optional<String> clue = room.inspect(Room.NOTEBOOK);
        if (clue.isPresent()) {
            clueFound = true;
            statusMessage = "Βρήκες τον κωδικό του σεντουκιού. "
                    + "Πήγαινε στη γωνία.";
            JOptionPane.showMessageDialog(
                    frame,
                    clue.get(),
                    "Παλιό Σημειωματάριο",
                    JOptionPane.INFORMATION_MESSAGE
            );
        }
    }

    private void inspectChest() {
        if (chestPuzzle.isSolved()) {
            statusMessage = "Το σεντούκι είναι ήδη ανοιχτό. "
                    + "Έχεις πάρει το κλειδί.";
            return;
        }
        if (!clueFound) {
            statusMessage = "Το σεντούκι είναι κλειδωμένο. "
                    + "Ψάξε πρώτα το σημειωματάριο.";
            JOptionPane.showMessageDialog(
                    frame,
                    statusMessage,
                    "Κλειδωμένο Σεντούκι",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        final String answer = (String) JOptionPane.showInputDialog(
                frame,
                chestPuzzle.getPrompt(),
                "Κλειδωμένο Σεντούκι",
                JOptionPane.QUESTION_MESSAGE,
                null,
                null,
                ""
        );
        if (answer == null) {
            return;
        }

        final AnswerResult result = chestPuzzle.check(answer);
        if (result.correct()) {
            hasKey = true;
            sessionItems.add(ItemType.LAB_KEY);
            statusMessage = result.message() + " Το σεντούκι άνοιξε! Πήρες το κλειδί της εξόδου.";
            JOptionPane.showMessageDialog(
                    frame, statusMessage, "Σωστός κωδικός", JOptionPane.INFORMATION_MESSAGE);
        } else {
            statusMessage = result.message() + " Διάβασε ξανά το σημειωματάριο.";
            JOptionPane.showMessageDialog(
                    frame, statusMessage, "Λάθος κωδικός", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void inspectExit() {
        final ExitAccessResult access = exitAccessService.checkAccess(
                demoExit,
                sessionItems
        );
        if (!access.allowed()) {
            statusMessage = "Η πόρτα είναι κλειδωμένη. Χρειάζεσαι το κλειδί "
                    + "από το σεντούκι.";
            JOptionPane.showMessageDialog(
                    frame, statusMessage, "Πόρτα Εξόδου", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        gameWon = true;
        statusMessage = "Η έξοδος ξεκλειδώθηκε. Ολοκλήρωσες την αποστολή!";
        gamePanel.repaint();
        JOptionPane.showMessageDialog(
                frame, statusMessage, "EscapeHub - Επιτυχία", JOptionPane.INFORMATION_MESSAGE);
        if (gameTimer != null) {
            gameTimer.stop();
        }
    }

    private final class GamePanel extends JPanel {

        @Override
        protected void paintComponent(final Graphics graphics) {
            super.paintComponent(graphics);
            final Graphics2D drawing = (Graphics2D) graphics.create();
            drawing.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            drawBackground(drawing);
            drawRoom(drawing);
            drawObjects(drawing);
            drawPlayer(drawing);
            drawHud(drawing);
            drawing.dispose();
        }

        private void drawBackground(final Graphics2D drawing) {
            drawing.setColor(new Color(12, 16, 23));
            drawing.fillRect(0, 0, getWidth(), getHeight());
            drawing.setColor(new Color(22, 29, 38));
            drawing.fillRoundRect(35, 58, 890, 540, 18, 18);
            drawing.setColor(new Color(93, 111, 125));
            drawing.setStroke(new BasicStroke(5));
            drawing.drawRoundRect(35, 58, 890, 540, 18, 18);
        }

        private void drawRoom(final Graphics2D drawing) {
            drawing.setColor(new Color(48, 57, 62));
            drawing.fillRect(75, 95, 810, 475);
            drawing.setColor(new Color(58, 68, 72));
            for (int x = 95; x < 885; x += 48) {
                drawing.drawLine(x, 145, x, 570);
            }
            for (int y = 160; y < 570; y += 48) {
                drawing.drawLine(75, y, 885, y);
            }
            drawing.setColor(new Color(105, 119, 124));
            drawing.setStroke(new BasicStroke(3));
            drawing.drawRect(75, 95, 810, 475);
            drawing.setFont(new Font("SansSerif", Font.BOLD, 17));
            drawing.setColor(new Color(220, 225, 220));
            drawing.drawString("ΕΓΚΑΤΑΛΕΛΕΙΜΜΕΝΟ ΕΡΓΑΣΤΗΡΙΟ", 95, 125);
        }

        private void drawObjects(final Graphics2D drawing) {
            drawDesk(drawing);
            drawNotebook(drawing);
            drawChest(drawing);
            drawExitDoor(drawing);

            final Target nearby = findNearbyTarget();
            if (nearby != null) {
                drawing.setColor(new Color(255, 219, 102));
                drawing.setStroke(new BasicStroke(3));
                drawing.draw(new Ellipse2D.Double(
                        nearby.getX() - 27,
                        nearby.getY() - 27,
                        54,
                        54
                ));
            }
        }

        private void drawDesk(final Graphics2D drawing) {
            drawing.setColor(new Color(25, 29, 31));
            drawing.fillRoundRect(145, 185, 280, 120, 12, 12);
            drawing.setColor(new Color(105, 78, 54));
            drawing.fillRoundRect(150, 180, 270, 110, 10, 10);
            drawing.setColor(new Color(155, 119, 79));
            drawing.setStroke(new BasicStroke(4));
            drawing.drawRoundRect(150, 180, 270, 110, 10, 10);
            drawing.setFont(new Font("SansSerif", Font.BOLD, 14));
            drawing.setColor(new Color(238, 222, 190));
            drawing.drawString("ΠΑΓΚΟΣ", 245, 174);
        }

        private void drawNotebook(final Graphics2D drawing) {
            drawing.setColor(new Color(25, 25, 24));
            drawing.fillRoundRect(270, 212, 45, 34, 4, 4);
            drawing.setColor(new Color(207, 198, 164));
            drawing.fillRoundRect(272, 207, 42, 32, 3, 3);
            drawing.setColor(new Color(95, 82, 60));
            drawing.drawLine(293, 209, 293, 237);
            drawing.setColor(new Color(133, 53, 45));
            drawing.drawLine(278, 216, 287, 216);
            drawing.drawLine(299, 216, 307, 216);
            drawing.setFont(new Font("SansSerif", Font.PLAIN, 12));
            drawing.setColor(new Color(235, 230, 214));
            drawing.drawString("Σημειωματάριο", 260, 265);
        }

        private void drawChest(final Graphics2D drawing) {
            drawing.setColor(new Color(27, 23, 19));
            drawing.fillRoundRect(675, 202, 150, 110, 10, 10);
            drawing.setColor(new Color(112, 66, 39));
            drawing.fillRoundRect(680, 195, 140, 110, 9, 9);
            drawing.setColor(new Color(174, 122, 68));
            drawing.setStroke(new BasicStroke(4));
            drawing.drawRoundRect(680, 195, 140, 110, 9, 9);
            drawing.drawLine(750, 198, 750, 302);
            drawing.setColor(new Color(226, 186, 93));
            drawing.fillRoundRect(738, 238, 24, 30, 5, 5);
            drawing.setColor(new Color(62, 47, 30));
            drawing.fillOval(746, 248, 8, 8);
            if (chestPuzzle.isSolved()) {
                drawing.setColor(new Color(174, 122, 68));
                drawing.fillRoundRect(690, 174, 120, 28, 7, 7);
                drawing.setColor(new Color(226, 186, 93));
                drawing.drawRoundRect(690, 174, 120, 28, 7, 7);
            }
            drawing.setFont(new Font("SansSerif", Font.PLAIN, 12));
            drawing.setColor(new Color(235, 230, 214));
            drawing.drawString("Σεντούκι", 718, 329);
        }

        private void drawExitDoor(final Graphics2D drawing) {
            drawing.setColor(new Color(20, 23, 25));
            drawing.fillRect(425, 88, 110, 58);
            drawing.setColor(hasKey ? new Color(72, 112, 81) : new Color(88, 65, 54));
            drawing.fillRect(430, 92, 100, 50);
            drawing.setColor(new Color(177, 149, 106));
            drawing.setStroke(new BasicStroke(3));
            drawing.drawRect(430, 92, 100, 50);
            drawing.setColor(new Color(224, 190, 98));
            drawing.fillOval(510, 114, 8, 8);
            drawing.setFont(new Font("SansSerif", Font.BOLD, 12));
            drawing.setColor(new Color(235, 230, 214));
            final String exitLabel;
            if (hasKey) {
                exitLabel = "ΕΞΟΔΟΣ - ΞΕΚΛΕΙΔΩΤΗ";
            } else {
                exitLabel = "ΕΞΟΔΟΣ - ΚΛΕΙΔΩΜΕΝΗ";
            }
            drawing.drawString(exitLabel, 405, 78);
        }

        private void drawPlayer(final Graphics2D drawing) {
            final int x = (int) Math.round(playerX);
            final int y = (int) Math.round(playerY);
            drawing.setColor(new Color(15, 18, 20, 150));
            drawing.fillOval(x - 19, y + 8, 38, 15);
            drawing.setColor(new Color(28, 71, 81));
            drawing.fillRoundRect(x - 20, y - 10, 14, 22, 7, 7);
            drawing.setColor(new Color(72, 178, 190));
            drawing.fillOval(x - 16, y - 17, 32, 34);
            drawing.setColor(new Color(181, 238, 236));
            drawing.fillOval(x - 8, y - 11, 16, 10);
            drawing.setColor(new Color(20, 55, 62));
            drawing.setStroke(new BasicStroke(2));
            drawing.drawOval(x - 16, y - 17, 32, 34);
        }

        private void drawHud(final Graphics2D drawing) {
            drawing.setColor(new Color(8, 12, 18, 225));
            drawing.fillRoundRect(55, 15, 850, 38, 10, 10);
            drawing.setFont(new Font("SansSerif", Font.BOLD, 14));
            drawing.setColor(new Color(235, 239, 235));
            drawing.drawString("ESCAPEHUB  |  " + room.getName(), 72, 39);
            drawing.setColor(hasKey ? new Color(138, 220, 145) : new Color(230, 199, 123));
            drawing.drawString(hasKey ? "ΚΛΕΙΔΙ: ΝΑΙ" : "ΚΛΕΙΔΙ: ΟΧΙ", 770, 39);

            drawing.setColor(new Color(8, 12, 18, 225));
            drawing.fillRoundRect(55, 582, 850, 42, 10, 10);
            drawing.setFont(new Font("SansSerif", Font.PLAIN, 14));
            drawing.setColor(new Color(235, 239, 235));
            drawing.drawString("Κίνηση: WASD / Βέλη     Αλληλεπίδραση: E", 72, 608);

            final Target nearby = findNearbyTarget();
            if (nearby != null) {
                drawing.setColor(new Color(255, 219, 102));
                drawing.drawString("Πατήστε E για: " + nearby.getLabel(), 545, 608);
            } else {
                drawing.setColor(new Color(194, 202, 205));
                drawing.drawString(statusMessage, 440, 608);
            }
        }
    }

    private enum Target {
        NOTEBOOK(NOTEBOOK_X, NOTEBOOK_Y, "Εξέταση σημειωματαρίου"),
        CHEST(CHEST_X, CHEST_Y, "Άνοιγμα σεντουκιού"),
        EXIT(DOOR_X, DOOR_Y, "Έλεγχος εξόδου");

        private final double x;
        private final double y;
        private final String label;

        Target(final double x, final double y, final String label) {
            this.x = x;
            this.y = y;
            this.label = label;
        }

        private double getX() {
            return x;
        }

        private double getY() {
            return y;
        }

        private String getLabel() {
            return label;
        }
    }

    /** Εκκίνηση του demo χωρίς εκκίνηση του Spring Boot server. */
    public static void main(final String[] args) {
        SwingUtilities.invokeLater(() -> new TopDownRoomView(new Room()).showWindow());
    }
}
