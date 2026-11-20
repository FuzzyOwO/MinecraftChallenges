package gg.itzkatze.katze.replay;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReplayPositionTest {
    private final UUID id = UUID.randomUUID(), world = UUID.randomUUID();

    @Test void interpolatesDroppedItemsAtFractionalPlaybackSpeedsWithoutSpinningAcrossYawZero() {
        var current = actor(world, 0, 350, null);
        var next = actor(world, 2, 10, null);
        var sampled = ReplayPosition.sample(current, next, 0.5);
        assertEquals(1, sampled.x());
        assertEquals(360, sampled.yaw());
        assertEquals(0.5, ReplayPosition.sample(current, next, 0.25).x());
    }

    @Test void dimensionChangesTeleportsAndMountChangesAreNotInterpolated() {
        var current = actor(world, 0, 0, null);
        assertEquals(0, ReplayPosition.sample(current, actor(UUID.randomUUID(), 2, 0, null), 0.5).x());
        assertEquals(0, ReplayPosition.sample(current, actor(world, 50, 0, null), 0.5).x());
        assertEquals(0, ReplayPosition.sample(current, actor(world, 2, 0, UUID.randomUUID()), 0.5).x());
    }

    private ReplayFrame.Actor actor(UUID dimension, double x, float yaw, UUID vehicle) {
        return new ReplayFrame.Actor(id, dimension, "ITEM", null, null, null, null, x, 64, 0, yaw, 0,
                "STANDING", false, false, List.of(), vehicle, false, 0, null, null);
    }
}
