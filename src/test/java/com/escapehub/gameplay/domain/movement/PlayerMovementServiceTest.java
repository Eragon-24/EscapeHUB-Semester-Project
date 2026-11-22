package com.escapehub.gameplay.domain.movement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class PlayerMovementServiceTest {

    @Test
    void shouldMovePlayerHorizontally() {
        final PlayerMovementService service = new PlayerMovementService(clearArea(), 2);

        final PlayerPosition result = service.move(
                new PlayerPosition(50, 50),
                new MovementInput(1, 0),
                5
        );

        assertEquals(new PlayerPosition(55, 50), result);
    }

    @Test
    void shouldNormalizeDiagonalMovement() {
        final PlayerMovementService service = new PlayerMovementService(clearArea(), 2);

        final PlayerPosition result = service.move(
                new PlayerPosition(50, 50),
                new MovementInput(1, 1),
                10
        );
        final double expectedDelta = 10 / Math.sqrt(2);

        assertEquals(50 + expectedDelta, result.x(), 0.000000001);
        assertEquals(50 + expectedDelta, result.y(), 0.000000001);
    }

    @Test
    void shouldStopAtMovementBounds() {
        final MovementArea area = new MovementArea(10, 10, 90, 90, List.of());
        final PlayerMovementService service = new PlayerMovementService(area, 2);

        final PlayerPosition result = service.move(
                new PlayerPosition(88, 50),
                new MovementInput(1, 0),
                5
        );

        assertEquals(new PlayerPosition(88, 50), result);
    }

    @Test
    void shouldBlockMovementThroughObstacle() {
        final MovementObstacle obstacle = new MovementObstacle(40, 40, 60, 60);
        final MovementArea area = new MovementArea(0, 0, 100, 100, List.of(obstacle));
        final PlayerMovementService service = new PlayerMovementService(area, 2);

        final PlayerPosition result = service.move(
                new PlayerPosition(35, 50),
                new MovementInput(1, 0),
                10
        );

        assertEquals(new PlayerPosition(35, 50), result);
    }

    @Test
    void shouldAllowMovementAlongAnObstacleWhenTheOtherAxisIsBlocked() {
        final MovementObstacle obstacle = new MovementObstacle(40, 40, 60, 60);
        final MovementArea area = new MovementArea(0, 0, 100, 100, List.of(obstacle));
        final PlayerMovementService service = new PlayerMovementService(area, 2);

        final PlayerPosition result = service.move(
                new PlayerPosition(35, 50),
                new MovementInput(1, 1),
                5
        );

        assertEquals(new PlayerPosition(35, 55), result);
    }

    @Test
    void shouldKeepPositionWhenNoDirectionIsRequested() {
        final PlayerPosition start = new PlayerPosition(50, 50);
        final PlayerMovementService service = new PlayerMovementService(clearArea(), 2);

        final PlayerPosition result = service.move(start, new MovementInput(0, 0), 5);

        assertSame(start, result);
    }

    @Test
    void shouldRejectInvalidMovementDirection() {
        assertThrows(IllegalArgumentException.class, () -> new MovementInput(2, 0));
    }

    @Test
    void shouldRejectNegativeMovementDistance() {
        final PlayerMovementService service = new PlayerMovementService(clearArea(), 2);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.move(new PlayerPosition(50, 50), new MovementInput(1, 0), -1)
        );
    }

    private MovementArea clearArea() {
        return new MovementArea(0, 0, 100, 100, List.of());
    }
}
