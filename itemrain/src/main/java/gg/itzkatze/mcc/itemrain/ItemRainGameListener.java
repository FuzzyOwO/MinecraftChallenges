package gg.itzkatze.mcc.itemrain;

import gg.itzkatze.mcc.MinecraftChallenges;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class ItemRainGameListener implements Listener {

    private static final List<Material> ITEM_MATERIALS = buildItemMaterials();

    private final Random random = new Random();
    private final Map<UUID, Long> nextGiveAtMillis = new HashMap<>();
    private int lastIntervalSeconds = -1;

    public void reset() {
        nextGiveAtMillis.clear();
        lastIntervalSeconds = -1;
    }

    public void runTask() {
        if (!MinecraftChallenges.getTimer().isRunning()) {
            reset();
            return;
        }

        int intervalSeconds = Math.max(1, ItemRainSettings.getIntervalSeconds());
        long now = System.currentTimeMillis();

        if (intervalSeconds != lastIntervalSeconds) {
            lastIntervalSeconds = intervalSeconds;
            long nextAt = now + intervalSeconds * 1000L;
            for (Player player : Bukkit.getOnlinePlayers()) {
                nextGiveAtMillis.put(player.getUniqueId(), nextAt);
            }
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            long nextAt = nextGiveAtMillis.getOrDefault(
                    player.getUniqueId(),
                    now + intervalSeconds * 1000L
            );
            if (now < nextAt) continue;
            giveRandomItem(player);
            nextGiveAtMillis.put(player.getUniqueId(), now + intervalSeconds * 1000L);
        }
    }

    private void giveRandomItem(Player player) {
        if (ITEM_MATERIALS.isEmpty()) return;
        Material material = ITEM_MATERIALS.get(random.nextInt(ITEM_MATERIALS.size()));
        ItemStack item = new ItemStack(material, 1);

        if (ItemRainSettings.isSkyDropEnabled()) {
            dropItemFromSky(player, item);
            return;
        }

        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }

    private void dropItemFromSky(Player player, ItemStack item) {
        int radius = Math.max(0, ItemRainSettings.getSkyDropRadius());
        double offsetX = radius == 0 ? 0 : random.nextDouble(-radius, radius);
        double offsetZ = radius == 0 ? 0 : random.nextDouble(-radius, radius);

        Location baseLocation = player.getLocation().clone().add(offsetX, 0, offsetZ);
        int maxHeight = player.getWorld().getMaxHeight() - 1;
        double spawnY = Math.min(maxHeight, Math.max(baseLocation.getY() + 16, player.getLocation().getY() + 12));
        Location dropLocation = new Location(
                player.getWorld(),
                baseLocation.getX(),
                spawnY,
                baseLocation.getZ()
        );

        player.getWorld().dropItem(dropLocation, item, droppedItem -> {
            droppedItem.setVelocity(new Vector(0, -0.15, 0));
            droppedItem.setCanMobPickup(false);
            droppedItem.setPickupDelay(10);
        });
    }

    private static List<Material> buildItemMaterials() {
        List<Material> materials = new ArrayList<>();
        for (Material material : Material.values()) {
            if (!material.isItem()) continue;
            if (material == Material.AIR) continue;
            materials.add(material);
        }
        return materials;
    }
}
