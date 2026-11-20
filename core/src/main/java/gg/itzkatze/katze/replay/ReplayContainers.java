package gg.itzkatze.katze.replay;

import org.bukkit.World;
import org.bukkit.block.Lidded;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

final class ReplayContainers {
    private final Function<UUID, World> worlds;
    private final Set<ReplayFrame.Container> open = new HashSet<>();

    ReplayContainers(Function<UUID, World> worlds) { this.worlds = worlds; }

    void render(List<ReplayFrame.Container> containers) {
        Set<ReplayFrame.Container> current = new HashSet<>(containers);
        for (var container : open) if (!current.contains(container)) animate(container, false);
        for (var container : current) if (!open.contains(container)) animate(container, true);
        open.clear();
        open.addAll(current);
    }

    void clear() {
        open.forEach(container -> animate(container, false));
        open.clear();
    }

    private void animate(ReplayFrame.Container container, boolean opening) {
        World world = worlds.apply(container.world());
        if (!opening && !world.isChunkLoaded(container.x() >> 4, container.z() >> 4)) return;
        if (world.getBlockAt(container.x(), container.y(), container.z()).getState(false) instanceof Lidded lid) {
            if (opening) lid.open();
            else lid.close();
        }
    }
}
