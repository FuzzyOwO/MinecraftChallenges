package gg.itzkatze.katze.replay;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

final class ReplayCracks {
    private final Player viewer;
    private final Function<UUID, World> worlds;
    private final Map<UUID, ReplayFrame.Mining> mining = new HashMap<>();

    ReplayCracks(Player viewer, Function<UUID, World> worlds) { this.viewer = viewer; this.worlds = worlds; }

    void render(List<ReplayFrame.Mining> breaking, int tick) {
        Set<UUID> current = new HashSet<>();
        for (ReplayFrame.Mining attempt : breaking) {
            if (!viewer.getWorld().equals(worlds.apply(attempt.world()))) continue;
            current.add(attempt.player());
            ReplayFrame.Mining old = mining.put(attempt.player(), attempt);
            if (old != null && (!old.world().equals(attempt.world()) || old.x() != attempt.x() || old.y() != attempt.y() || old.z() != attempt.z())) send(old, 0);
            if (!attempt.equals(old) || tick % 100 == 0) send(attempt, attempt.progress());
        }
        for (UUID id : new ArrayList<>(mining.keySet())) if (!current.contains(id)) send(mining.remove(id), 0);
    }

    private void send(ReplayFrame.Mining attempt, float progress) {
        World world = worlds.apply(attempt.world());
        if (viewer.getWorld().equals(world)) viewer.sendBlockDamage(new Location(world, attempt.x(), attempt.y(), attempt.z()), progress, attempt.player().hashCode());
    }

    void clear() { mining.values().forEach(attempt -> send(attempt, 0)); mining.clear(); }
}
