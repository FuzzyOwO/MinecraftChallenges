package gg.itzkatze.mcc.randomizer;

import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class RandomizerGameListener implements Listener {

    private final Random random = new Random();
    private final List<Material> itemPool;
    private final Map<Material, Material> blockDropMap = new EnumMap<>(Material.class);
    private final Map<Material, Material> mobDropMap = new EnumMap<>(Material.class);
    private final Map<Material, Material> chestLootMap = new EnumMap<>(Material.class);
    private final Map<UUID, Map<Material, Material>> playerBlockDropMaps = new java.util.HashMap<>();
    private final Map<UUID, Map<Material, Material>> playerMobDropMaps = new java.util.HashMap<>();
    private final Map<UUID, Map<Material, Material>> playerChestLootMaps = new java.util.HashMap<>();

    public RandomizerGameListener() {
        this.itemPool = buildItemPool();
    }

    public void resetMappings() {
        blockDropMap.clear();
        mobDropMap.clear();
        chestLootMap.clear();
        playerBlockDropMaps.clear();
        playerMobDropMaps.clear();
        playerChestLootMaps.clear();
    }

    @EventHandler
    public void onBlockDrop(BlockDropItemEvent event) {
        if (!RandomizerSettings.isBlockDropsEnabled()) return;
        if (event.getItems().isEmpty()) return;

        Material blockType = event.getBlockState().getType();
        Material replacement = getOrCreate(getMapping(playerBlockDropMaps, blockDropMap, event.getPlayer()), blockType);
        for (Item item : event.getItems()) {
            ItemStack stack = item.getItemStack();
            ItemStack replaced = new ItemStack(replacement, stack.getAmount());
            item.setItemStack(replaced);
        }
    }

    @EventHandler
    public void onMobDeath(EntityDeathEvent event) {
        if (!RandomizerSettings.isMobDropsEnabled()) return;
        List<ItemStack> drops = event.getDrops();
        if (drops.isEmpty()) return;

        for (int i = 0; i < drops.size(); i++) {
            ItemStack drop = drops.get(i);
            if (drop == null || drop.getType().isAir()) continue;
            Material replacement = getOrCreate(getMapping(playerMobDropMaps, mobDropMap, event.getEntity().getKiller()), drop.getType());
            drops.set(i, new ItemStack(replacement, drop.getAmount()));
        }
    }

    @EventHandler
    public void onLootGenerate(LootGenerateEvent event) {
        if (!RandomizerSettings.isChestLootEnabled()) return;
        List<ItemStack> loot = event.getLoot();
        if (loot.isEmpty()) return;

        for (int i = 0; i < loot.size(); i++) {
            ItemStack stack = loot.get(i);
            if (stack == null || stack.getType().isAir()) continue;
            Player player = event.getEntity() instanceof Player p ? p : null;
            Material replacement = getOrCreate(getMapping(playerChestLootMaps, chestLootMap, player), stack.getType());
            loot.set(i, new ItemStack(replacement, stack.getAmount()));
        }
    }

    private Map<Material, Material> getMapping(Map<UUID, Map<Material, Material>> playerMappings,
                                               Map<Material, Material> sharedMapping,
                                               Player player) {
        if (RandomizerSettings.isSharedMappingsEnabled() || player == null) {
            return sharedMapping;
        }
        return playerMappings.computeIfAbsent(player.getUniqueId(), ignored -> new EnumMap<>(Material.class));
    }

    private Material getOrCreate(Map<Material, Material> map, Material source) {
        return map.computeIfAbsent(source, key -> {
            if (itemPool.isEmpty()) return key;
            Material chosen = itemPool.get(random.nextInt(itemPool.size()));
            if (chosen == key && itemPool.size() > 1) {
                chosen = itemPool.get((itemPool.indexOf(chosen) + 1) % itemPool.size());
            }
            return chosen;
        });
    }

    private static List<Material> buildItemPool() {
        List<Material> materials = new ArrayList<>();
        for (Material material : Material.values()) {
            if (!material.isItem()) continue;
            if (material.isAir()) continue;
            materials.add(material);
        }
        Collections.shuffle(materials);
        return materials;
    }
}
