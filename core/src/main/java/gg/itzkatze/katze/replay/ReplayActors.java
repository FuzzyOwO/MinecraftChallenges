package gg.itzkatze.katze.replay;

import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

final class ReplayActors {
    private ReplayActors() {}

    static List<ReplayFrame.Actor> capture(List<Player> participants, Collection<Chunk> chunks, Function<Entity, ReplayFrame.Actor> snapshot) {
        return capture(participants, chunks, List.of(), snapshot);
    }

    static List<ReplayFrame.Actor> capture(List<Player> participants, Collection<Chunk> chunks, Collection<Entity> tracked, Function<Entity, ReplayFrame.Actor> snapshot) {
        Set<UUID> seen = new HashSet<>();
        List<ReplayFrame.Actor> actors = new ArrayList<>();
        for (Player player : participants) {
            capture(player, seen, actors, snapshot);
        }
        for (Entity entity : tracked) if (!(entity instanceof Player)) capture(entity, seen, actors, snapshot);
        for (Chunk chunk : chunks) for (Entity entity : chunk.getEntities()) {
            if (entity instanceof Player) continue;
            if (!(entity instanceof LivingEntity) && entity.getType().getEntityClass() == null) continue;
            capture(entity, seen, actors, snapshot);
        }
        return actors;
    }

    private static void capture(Entity entity, Set<UUID> seen, List<ReplayFrame.Actor> actors, Function<Entity, ReplayFrame.Actor> snapshot) {
        if (!seen.add(entity.getUniqueId())) return;
        actors.add(snapshot.apply(entity));
        Entity vehicle = entity.getVehicle();
        if (vehicle != null && !(vehicle instanceof Player)) capture(vehicle, seen, actors, snapshot);
    }
}
