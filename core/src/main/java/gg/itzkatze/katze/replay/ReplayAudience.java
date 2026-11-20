package gg.itzkatze.katze.replay;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class ReplayAudience<T> {
    private final Set<T> viewers = new LinkedHashSet<>();
    private volatile boolean closed;

    void add(T viewer) {
        if (closed) throw new IllegalStateException("Replay is closed");
        viewers.add(viewer);
    }

    boolean remove(T viewer) {
        if (!viewers.remove(viewer)) return false;
        closed = viewers.isEmpty();
        return closed;
    }

    List<T> viewers() { return List.copyOf(viewers); }
    boolean closed() { return closed; }
}
