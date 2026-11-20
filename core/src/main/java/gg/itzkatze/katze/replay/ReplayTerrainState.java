package gg.itzkatze.katze.replay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class ReplayTerrainState {
    private record Position(UUID world, int x, int y, int z) {}
    private final Map<Position, ReplayFrame.BlockChange> blocks = new HashMap<>();

    ReplayTerrainState copy() {
        var copy = new ReplayTerrainState();
        copy.blocks.putAll(blocks);
        return copy;
    }

    void apply(ReplayFrame.BlockChange block, boolean backwards) {
        Position position = new Position(block.world(), block.x(), block.y(), block.z());
        ReplayFrame.BlockChange previous = blocks.get(position);
        String before = previous == null ? backwards ? block.after() : block.before() : previous.before();
        String after = backwards ? block.before() : block.after();
        if (before.equals(after)) blocks.remove(position);
        else blocks.put(position, new ReplayFrame.BlockChange(block.world(), block.x(), block.y(), block.z(), before, after));
    }

    List<ReplayFrame.BlockChange> snapshot() { return List.copyOf(blocks.values()); }

    List<ReplayFrame.BlockChange> changesTo(ReplayTerrainState target) {
        Set<Position> positions = new HashSet<>(blocks.keySet());
        positions.addAll(target.blocks.keySet());
        List<ReplayFrame.BlockChange> changes = new ArrayList<>();
        for (Position position : positions) {
            ReplayFrame.BlockChange current = blocks.get(position), next = target.blocks.get(position);
            String before = current == null ? next.before() : current.after();
            String after = next == null ? current.before() : next.after();
            if (!before.equals(after)) changes.add(new ReplayFrame.BlockChange(position.world(), position.x(), position.y(), position.z(), before, after));
        }
        return changes;
    }
}
