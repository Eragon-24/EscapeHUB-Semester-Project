package com.escapehub.user.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.escapehub.user.application.DuplicateUserException;
import com.escapehub.user.domain.User;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
    void saveRejectsDuplicateUsernameIgnoringCase() {
        repository.save("Marios", "a@example.com", "hash");

        final DuplicateUserException ex = assertThrows(DuplicateUserException.class,
                () -> repository.save("MARIOS", "b@example.com", "hash"));
        assertEquals("username", ex.getField());
    }

    @Test
    void saveRejectsDuplicateEmailIgnoringCase() {
        repository.save("one", "Same@Example.com", "hash");

        final DuplicateUserException ex = assertThrows(DuplicateUserException.class,
                () -> repository.save("two", "same@example.com", "hash"));
        assertEquals("email", ex.getField());
    }

    @Test
    void rejectedSaveDoesNotConsumeAnId() {
        repository.save("one", "one@example.com", "hash");
        assertThrows(DuplicateUserException.class, () -> repository.save("one", "x@example.com", "hash"));

        assertEquals(2, repository.save("two", "two@example.com", "hash").id());
    }

    @Test
    void concurrentSavesOfSameUserCreateExactlyOneAccount() throws Exception {
        final int attempts = 20;
        final ExecutorService pool = Executors.newFixedThreadPool(attempts);
        final CountDownLatch start = new CountDownLatch(1);
        final List<Future<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            results.add(pool.submit(() -> {
                start.await();
                try {
                    repository.save("racer", "racer@example.com", "hash");
                    return true;
                } catch (DuplicateUserException e) {
                    return false;
                }
            }));
        }
        start.countDown();
        int created = 0;
        for (final Future<Boolean> result : results) {
            if (result.get()) {
                created++;
            }
        }
        pool.shutdown();

        assertEquals(1, created);
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
