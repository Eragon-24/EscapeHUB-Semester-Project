package com.escapehub.user.domain;

/**
 * US-007: λογαριασμός χρήστη της πλατφόρμας.
 *
 * @param id μοναδικό αναγνωριστικό
 * @param username όνομα χρήστη
 * @param email διεύθυνση email
 * @param passwordHash κρυπτογραφημένος (hash) κωδικός, ποτέ ο κωδικός σε απλό κείμενο
 */
public record User(long id, String username, String email, String passwordHash) {
}
