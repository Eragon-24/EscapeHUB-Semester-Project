package com.escapehub.evidence.application;

/**
 * US-020: Command carrying the details of a discovered clue to add to a shared journal.
 *
 * @param evidenceId unique identifier of the eligible clue
 * @param title optional custom display title (falls back to catalog default if blank)
 * @param description optional custom description (falls back to catalog default if blank)
 * @param source optional custom source label (falls back to catalog default if blank)
 */
public record AddEvidenceCommand(
        String evidenceId,
        String title,
        String description,
        String source) {
}