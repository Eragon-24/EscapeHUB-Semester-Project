package com.escapehub.gameplay.domain;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 * US-001: Απλό Desktop UI παραθύρου για την προβολή του δωματίου στο Demo v0.1.
 */
public class RoomView {

    private final Room room;

    /**
     * Δημιουργεί την προβολή για το συγκεκριμένο δωμάτιο.
     *
     * @param room το δωμάτιο προς εμφάνιση
     */
    public RoomView(final Room room) {
        this.room = room;
    }

    /**
     * Εμφανίζει το παράθυρο του δωματίου με τα επιλέξιμα αντικείμενα.
     */
    public void showWindow() {
        final JFrame frame = new JFrame("EscapeHub Demo v0.1 - " + room.getName());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(560, 320);
        frame.setLayout(new BorderLayout(10, 10));

        final JLabel titleLabel = new JLabel(room.getName());
        titleLabel.setBorder(BorderFactory.createEmptyBorder(12, 16, 0, 16));
        frame.add(titleLabel, BorderLayout.NORTH);

        final JTextArea descriptionArea = new JTextArea(room.getDescription());
        descriptionArea.setEditable(false);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        frame.add(descriptionArea, BorderLayout.CENTER);

        final JPanel objectsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        for (final String objectName : room.getSelectableObjects()) {
            final JButton objectButton = new JButton(objectName);
            objectButton.addActionListener(e -> JOptionPane.showMessageDialog(
                    frame,
                    "Επέλεξες: " + objectName
            ));
            objectsPanel.add(objectButton);
        }
        frame.add(objectsPanel, BorderLayout.SOUTH);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /**
     * Εκκίνηση του UI του δωματίου για επίδειξη στο Demo v0.1.
     *
     * @param args παράμετροι γραμμής εντολών
     */
    public static void main(final String[] args) {
        SwingUtilities.invokeLater(() -> new RoomView(new Room()).showWindow());
    }
}