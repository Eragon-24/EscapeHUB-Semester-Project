package com.escapehub.evidence.domain;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * US-020: Registry of eligible clues that can be discovered and added to a session's
 * shared evidence journal.
 */
public final class EvidenceCatalog {

    /**
     * Metadata template for an eligible discoverable clue.
     *
     * @param evidenceId unique clue identifier
     * @param defaultTitle default display title
     * @param defaultDescription default clue description
     * @param defaultSource in-room object or location where the clue is found
     */
    public record ClueDefinition(
            String evidenceId,
            String defaultTitle,
            String defaultDescription,
            String defaultSource) {
    }

    private static final Map<String, ClueDefinition> ELIGIBLE_CLUES = Map.of(
            "LAB_NOTEBOOK",
            new ClueDefinition(
                    "LAB_NOTEBOOK",
                    "Σημειωματάριο Εργαστηρίου",
                    "Χειρόγραφες σημειώσεις με τον κωδικό πρόσβασης του σεντουκιού.",
                    "Πάγκος Εργαστηρίου"
            ),
            "CHEMICAL_FORMULA",
            new ClueDefinition(
                    "CHEMICAL_FORMULA",
                    "Χημικός Τύπος",
                    "Αναλογία αντιδραστηρίων σημειωμένη στο περιθώριο του πίνακα.",
                    "Πίνακας Εργαστηρίου"
            ),
            "CHEST_INSCRIPTION",
            new ClueDefinition(
                    "CHEST_INSCRIPTION",
                    "Χάραγμα Σεντουκιού",
                    "Τρία σύμβολα χαραγμένα δίπλα στην κλειδαριά του σεντουκιού.",
                    "Σεντούκι"
            ),
            "EXIT_SCHEMATIC",
            new ClueDefinition(
                    "EXIT_SCHEMATIC",
                    "Σχεδιάγραμμα Εξόδου",
                    "Διάγραμμα που δείχνει τον μηχανισμό απασφάλισης της πόρτας.",
                    "Έξοδος"
            )
    );

    private EvidenceCatalog() {
    }

    /**
     * Looks up an eligible clue definition by its identifier.
     *
     * @param evidenceId candidate clue identifier
     * @return matching clue definition when eligible
     */
    public static Optional<ClueDefinition> findEligible(final String evidenceId) {
        if (evidenceId == null || evidenceId.isBlank()) {
            return Optional.empty();
        }
        final String normalized = evidenceId.trim().toUpperCase(Locale.ROOT);
        return Optional.ofNullable(ELIGIBLE_CLUES.get(normalized));
    }
}