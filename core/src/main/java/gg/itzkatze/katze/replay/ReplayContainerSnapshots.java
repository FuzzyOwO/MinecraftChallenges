package gg.itzkatze.katze.replay;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.Lidded;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class ReplayContainerSnapshots {
    private final Map<UUID, ReplayFrame.Container> enderChests = new HashMap<>();

    record View(UUID player, World world, InventoryHolder holder, boolean enderChest) {}

    void interact(UUID player, Block block) {
        if (block.getType() == Material.ENDER_CHEST) enderChests.put(player, position(block));
    }

    List<ReplayFrame.Container> capture(List<Player> players) {
        return captureViews(players.stream().map(player -> {
            var inventory = player.getOpenInventory().getTopInventory();
            return new View(player.getUniqueId(), player.getWorld(), inventory.getHolder(false), inventory.getType() == InventoryType.ENDER_CHEST);
        }).toList());
    }

    List<ReplayFrame.Container> captureViews(List<View> views) {
        Set<ReplayFrame.Container> open = new LinkedHashSet<>();
        for (View view : views) {
            if (view.enderChest()) {
                var chest = enderChests.get(view.player());
                if (chest != null && chest.world().equals(view.world().getUID()) && view.world().isChunkLoaded(chest.x() >> 4, chest.z() >> 4)
                        && view.world().getBlockAt(chest.x(), chest.y(), chest.z()).getType() == Material.ENDER_CHEST) open.add(chest);
            } else {
                enderChests.remove(view.player());
                add(view.holder(), open);
            }
        }
        enderChests.keySet().retainAll(views.stream().map(View::player).toList());
        return List.copyOf(open);
    }

    private void add(InventoryHolder holder, Set<ReplayFrame.Container> open) {
        if (holder instanceof DoubleChest chest) {
            add(chest.getLeftSide(), open);
            add(chest.getRightSide(), open);
        } else if (holder instanceof BlockState state && state instanceof Lidded && state.isPlaced()) {
            open.add(position(state.getBlock()));
        }
    }

    private ReplayFrame.Container position(Block block) { return new ReplayFrame.Container(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ()); }
}
