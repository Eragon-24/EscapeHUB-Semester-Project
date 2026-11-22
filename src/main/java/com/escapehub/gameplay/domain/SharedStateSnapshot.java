package com.escapehub.gameplay.domain;

import java.util.Map;
import java.util.UUID;

/**
 * US-015: the complete official shared state of a session at one version. It is sent to a participant when
 * they (re)connect, so that nobody has to refresh the page after a lost connection.
 *
 * @param sessionId the game session
 * @param version the version of the state; updates with a version not greater than this one are already included
 * @param values all state entries
 */
public record SharedStateSnapshot(UUID sessionId, long version, Map<String, String> values) {
}
