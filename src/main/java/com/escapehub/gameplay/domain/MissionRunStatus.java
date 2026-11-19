package com.escapehub.gameplay.domain;

/**
 * US-015: η κατάσταση μιας εκτέλεσης αποστολής (MissionRun).
 */
public enum MissionRunStatus {

    /** Η εκτέλεση είναι σε εξέλιξη. */
    ACTIVE("Ενεργή"),

    /** Η εκτέλεση ολοκληρώθηκε. */
    COMPLETED("Ολοκληρωμένη");

    private final String label;

    MissionRunStatus(final String label) {
        this.label = label;
    }

    /**
     * Επιστρέφει την ελληνική ετικέτα της κατάστασης για εμφάνιση.
     *
     * @return η ετικέτα
     */
    public String getLabel() {
        return label;
    }
}
