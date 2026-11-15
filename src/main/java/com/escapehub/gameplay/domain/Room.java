package com.escapehub.gameplay.domain;

import java.util.List;
import java.util.Optional;

/**
 * US-001: Domain model για την προβολή ενός δωματίου στο Demo v0.1.
 * Περιέχει το όνομα, την περιγραφή και τα διαδραστικά αντικείμενα του χώρου
 * (σημειωματάριο, σεντούκι, πόρτα εξόδου).
 */
public class Room {

    /** Όνομα του σημειωματαρίου στη λίστα αντικειμένων. */
    public static final String NOTEBOOK = "Παλιό Σημειωματάριο";

    /** US-002: η ένδειξη που αποκαλύπτεται όταν επιλεγεί το σημειωματάριο (εξάψηφος κωδικός). */
    public static final String NOTEBOOK_CLUE =
            "Στην τελευταία σελίδα είναι γραμμένος ένας εξάψηφος κωδικός: 4-8-3-9-2-0. "
                    + "Δίπλα του, υπογραμμισμένο: «Το σεντούκι στη γωνία».";

    private final String name;
    private final String description;
    private final List<String> selectableObjects;

    /**
     * Δημιουργεί το αρχικό δωμάτιο του εγκαταλελειμμένου εργαστηρίου για το Demo v0.1.
     */
    public Room() {
        this(
                "Εγκαταλελειμμένο Εργαστήριο",
                "Βρίσκεσαι σε ένα σκοτεινό, σκονισμένο εργαστήριο. "
                        + "Πάνω στον πάγκο υπάρχει ένα παλιό σημειωματάριο, "
                        + "στη γωνία ένα κλειδωμένο σεντούκι με εξάψηφη κλειδαριά "
                        + "και απέναντι η βαριά πόρτα της εξόδου.",
                List.of("Παλιό Σημειωματάριο", "Κλειδωμένο Σεντούκι", "Πόρτα Εξόδου")
        );
    }

    /**
     * Δημιουργεί ένα δωμάτιο με συγκεκριμένο όνομα, περιγραφή και αντικείμενα.
     *
     * @param name το όνομα του δωματίου
     * @param description η περιγραφή του δωματίου
     * @param selectableObjects τα αντικείμενα που μπορεί να επιλέξει ο παίκτης
     */
    public Room(
            final String name,
            final String description,
            final List<String> selectableObjects) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Room name must not be blank");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Room description must not be blank");
        }
        if (selectableObjects == null || selectableObjects.isEmpty()) {
            throw new IllegalArgumentException("Room must contain at least one object");
        }
        this.name = name;
        this.description = description;
        this.selectableObjects = List.copyOf(selectableObjects);
    }

    /**
     * Επιστρέφει το όνομα του δωματίου.
     *
     * @return το όνομα του δωματίου
     */
    public String getName() {
        return name;
    }

    /**
     * Επιστρέφει την περιγραφή του δωματίου.
     *
     * @return η περιγραφή του δωματίου
     */
    public String getDescription() {
        return description;
    }

    /**
     * Επιστρέφει τη λίστα με τα επιλέξιμα αντικείμενα του δωματίου.
     *
     * @return αμετάβλητη λίστα αντικειμένων
     */
    public List<String> getSelectableObjects() {
        return selectableObjects;
    }

    /**
     * US-002: Επιλογή αντικειμένου. Επιστρέφει την ένδειξη που αποκαλύπτει,
     * ή κενό αν το αντικείμενο δεν κρύβει ένδειξη.
     *
     * @param objectName το όνομα του αντικειμένου που επέλεξε ο παίκτης
     * @return η ένδειξη, αν υπάρχει
     */
    public Optional<String> inspect(final String objectName) {
        if (NOTEBOOK.equals(objectName)) {
            return Optional.of(NOTEBOOK_CLUE);
        }
        return Optional.empty();
    }
}