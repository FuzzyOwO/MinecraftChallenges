package gg.itzkatze.katze.replay;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class ReplaySeekTest {
    private final UUID world = UUID.randomUUID();

    @Test void backwardsSeekOnlyAppliesTheFinalStateOfEachCrossedBlock() {
        var frames = List.of(frame(0), frame(20, block(0, "air", "stone")),
                frame(40, block(0, "stone", "gold"), block(1, "air", "stone")),
                frame(60, block(0, "gold", "diamond"), block(1, "stone", "air")));
        var seek = seek(frames, 60, 3, 20);
        List<ReplayFrame.BlockChange> changes = new ArrayList<>();
        assertTrue(seek.apply(128, Long.MAX_VALUE, () -> 0, changes::add));
        assertEquals(List.of(block(0, "diamond", "stone")), changes);
        assertEquals(20, seek.timeline().position());
    }

    @Test void forwardSeekCoalescesChangesAndSkipsBlocksThatEndUnchanged() {
        var frames = List.of(frame(0), frame(20, block(0, "air", "stone")),
                frame(40, block(0, "stone", "gold"), block(1, "air", "stone")),
                frame(60, block(1, "stone", "air")));
        var seek = seek(frames, 60, 0, 60);
        List<ReplayFrame.BlockChange> changes = new ArrayList<>();
        assertTrue(seek.apply(128, Long.MAX_VALUE, () -> 0, changes::add));
        assertEquals(List.of(block(0, "air", "gold")), changes);
    }

    @Test void terrainUpdatesAreBoundedByBothBlockCountAndElapsedTime() {
        var blocks = IntStream.range(0, 300).mapToObj(index -> block(index, "air", "stone")).toList();
        var seek = seek(List.of(frame(0), new ReplayFrame(20, List.of(), blocks, List.of())), 20, 0, 20);
        List<ReplayFrame.BlockChange> applied = new ArrayList<>();
        assertFalse(seek.apply(128, Long.MAX_VALUE, () -> 0, applied::add));
        assertEquals(128, applied.size());
        AtomicLong clock = new AtomicLong();
        assertFalse(seek.apply(128, 3, clock::get, block -> { applied.add(block); clock.incrementAndGet(); }));
        assertEquals(131, applied.size());
        assertFalse(seek.apply(128, Long.MAX_VALUE, () -> 0, applied::add));
        assertTrue(seek.apply(128, Long.MAX_VALUE, () -> 0, applied::add));
        assertEquals(blocks, applied);
    }

    @Test void sameFrameAndFractionalSeeksDoNotChangeTerrain() {
        var frames = List.of(frame(0, block(0, "air", "stone")), frame(20));
        var seek = seek(frames, 40, 0, 10);
        assertTrue(seek.apply(128, Long.MAX_VALUE, () -> 0, block -> fail()));
        assertEquals(10, seek.timeline().position());
        assertEquals(0, seek.timeline().currentFrame().tick());
    }

    private ReplaySeek seek(List<ReplayFrame> frames, int duration, int fromIndex, int target) {
        var source = new ReplayTimeline(frames, duration);
        source.moveTo(frames.get(fromIndex).tick(), (block, reverse) -> {});
        var seek = ReplaySeek.prepare(source, target);
        assertEquals(frames.get(fromIndex).tick(), source.position());
        return seek;
    }

    private ReplayFrame frame(int tick, ReplayFrame.BlockChange... blocks) {
        return new ReplayFrame(tick, List.of(), List.of(blocks), List.of());
    }

    private ReplayFrame.BlockChange block(int x, String before, String after) {
        return new ReplayFrame.BlockChange(world, x, 64, 0, before, after);
    }
}
