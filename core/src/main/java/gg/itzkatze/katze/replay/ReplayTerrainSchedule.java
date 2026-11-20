package gg.itzkatze.katze.replay;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ReplayTerrainSchedule {
    private static final int REFRESH_INTERVAL = 200;
    private static final int REFRESH_BUDGET = 2;
    private final Map<ReplayStore.ChunkKey, Integer> captured = new HashMap<>();

    boolean contains(ReplayStore.ChunkKey key) { return captured.containsKey(key); }

    List<ReplayStore.ChunkKey> capture(Collection<ReplayStore.ChunkKey> visible, int tick, boolean finishing, boolean refresh) {
        List<ReplayStore.ChunkKey> selected = new ArrayList<>();
        int remaining = REFRESH_BUDGET;
        for (var key : visible) {
            Integer previous = captured.get(key);
            if (previous == null || finishing || (refresh && remaining > 0 && tick - previous >= REFRESH_INTERVAL)) {
                captured.put(key, tick);
                selected.add(key);
                if (previous != null && !finishing) remaining--;
            }
        }
        return selected;
    }
}
