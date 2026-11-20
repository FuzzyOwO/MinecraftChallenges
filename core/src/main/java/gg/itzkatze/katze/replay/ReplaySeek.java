package gg.itzkatze.katze.replay;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

final class ReplaySeek {
    private final ReplayTimeline timeline;
    private final Iterator<ReplayFrame.BlockChange> blocks;
    private Set<UUID> deferred = Set.of();

    private record Position(UUID world, int x, int y, int z) {}

    private ReplaySeek(ReplayTimeline timeline, List<ReplayFrame.BlockChange> blocks) {
        this.timeline = timeline;
        this.blocks = blocks.iterator();
    }

    static ReplaySeek prepare(ReplayTimeline source, int target) {
        ReplayTimeline timeline = source.copy();
        Map<Position, ReplayFrame.BlockChange> changed = new LinkedHashMap<>();
        timeline.moveTo(target, (block, backwards) -> {
            Position position = new Position(block.world(), block.x(), block.y(), block.z());
            String before = backwards ? block.after() : block.before();
            String after = backwards ? block.before() : block.after();
            ReplayFrame.BlockChange previous = changed.get(position);
            changed.put(position, new ReplayFrame.BlockChange(block.world(), block.x(), block.y(), block.z(),
                    previous == null ? before : previous.before(), after));
        });
        return new ReplaySeek(timeline, changed.values().stream().filter(block -> !block.before().equals(block.after())).toList());
    }

    static ReplaySeek prepare(ReplayTimeline source, int target, ReplayTerrainState physical, ReplayCheckpoint checkpoint,
                              List<ReplayFrame> frames, int duration) {
        ReplayTimeline timeline = checkpoint != null && target >= checkpoint.frame().tick() && source.position() < checkpoint.frame().tick()
                ? new ReplayTimeline(frames, duration, checkpoint) : source.copy();
        timeline.moveTo(target, (block, backwards) -> {});
        Set<UUID> visible = timeline.currentFrame().actors().stream().map(ReplayFrame.Actor::world).collect(Collectors.toSet());
        List<ReplayFrame.BlockChange> changes = physical.changesTo(timeline.terrain());
        if (timeline.currentFrame().actors().stream().noneMatch(actor -> actor.type().equals("PLAYER"))) changes.forEach(block -> visible.add(block.world()));
        var seek = new ReplaySeek(timeline, changes.stream().filter(block -> visible.contains(block.world())).toList());
        seek.deferred = changes.stream().map(ReplayFrame.BlockChange::world).filter(world -> !visible.contains(world)).collect(Collectors.toCollection(HashSet::new));
        return seek;
    }

    Set<UUID> deferredWorlds() { return deferred; }

    boolean apply(int limit, long budgetNanos, LongSupplier clock, Consumer<ReplayFrame.BlockChange> apply) {
        long start = clock.getAsLong();
        for (int count = 0; blocks.hasNext() && count < limit; count++) {
            apply.accept(blocks.next());
            if (clock.getAsLong() - start >= budgetNanos) break;
        }
        return !blocks.hasNext();
    }

    ReplayTimeline timeline() { return timeline; }
}
