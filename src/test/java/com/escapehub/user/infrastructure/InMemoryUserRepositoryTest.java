package com.escapehub.user.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.escapehub.user.domain.User;
import org.junit.jupiter.api.Test;

class InMemoryUserRepositoryTest {

    private final InMemoryUserRepository repository = new InMemoryUserRepository();

    @Test
    void savedUsersGetIncreasingIds() {
        final User first = repository.save("one", "one@example.com", "hash");
        final User second = repository.save("two", "two@example.com", "hash");

        assertEquals(1, first.id());
        assertEquals(2, second.id());
    }

    @Test
    void findsByUsernameAndEmailIgnoringCase() {
        repository.save("Marios", "Marios@Example.com", "hash");

        assertTrue(repository.findByUsername("mARIOS").isPresent());
        assertTrue(repository.findByEmail("marios@example.COM").isPresent());
        assertTrue(repository.findByUsername("nobody").isEmpty());
        assertTrue(repository.findByEmail("nobody@example.com").isEmpty());
    }
}
