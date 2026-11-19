package com.escapehub.gameplay.application;

import com.escapehub.gameplay.domain.MissionRun;
import java.util.List;

/**
 * US-015: οι εκτελέσεις ενός παίκτη ομαδοποιημένες ανά κατηγορία.
 *
 * @param active οι ενεργές εκτελέσεις (οι πιο πρόσφατες πρώτα)
 * @param completed οι ολοκληρωμένες εκτελέσεις (οι πιο πρόσφατες πρώτα)
 */
public record MissionRunOverview(List<MissionRun> active, List<MissionRun> completed) {
}
