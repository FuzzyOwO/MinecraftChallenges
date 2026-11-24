package gg.itzkatze.katze.randomizer;

import gg.itzkatze.katze.challenges.ChallengeManager;
import gg.itzkatze.katze.challenges.ChallengeType;
import gg.itzkatze.katze.game.SessionPersistence;
import gg.itzkatze.katze.game.entry.GameEntryService;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.team.KatzeTeam;
import gg.itzkatze.katze.util.WorldStorage;
import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import io.papermc.paper.event.block.BlockBreakBlockEvent;
import io.papermc.paper.event.block.PlayerShearBlockEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.FluidLevelChangeEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockShearEntityEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PiglinBarterEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerHarvestBlockEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;

public class RandomizerGameListener implements Listener {

    private static final String FILE_NAME = "randomizer_mappings.json";

    private final RandomizerMappings mappings;
    private final BiFunction<Material, Integer, ItemStack> itemFactory;
    private final Map<BlockPos, ExplosionDrops> explosionDrops = new HashMap<>();
    private final Map<Location, List<ItemStack>> brokenContainers = new HashMap<>();
    // Blocks destroyed this tick whose drop is spawned without a drop event (value = responsible player or null).
    private final Map<BlockPos, Player> pendingDrops = new HashMap<>();
    // Blocks this tick whose drops were already randomized through a drop event.
    private final Set<BlockPos> handledBlocks = new HashSet<>();
    private final Set<UUID> handledDropItems = new HashSet<>();
    private int blockTick = -1;
    private int explosionTick = -1;
    private int containerTick = -1;

    private record ExplosionDrops(Player player, List<ItemStack> contents) {}

    /** Block coordinates only. Location.equals also compares yaw/pitch, which spawned items randomize. */
    private record BlockPos(UUID world, int x, int y, int z) {
        static BlockPos of(Location location) {
            return new BlockPos(location.getWorld() == null ? null : location.getWorld().getUID(),
                    location.getBlockX(), location.getBlockY(), location.getBlockZ());
        }
    }

    public RandomizerGameListener() {
        this(buildItemPool());
    }

    RandomizerGameListener(List<Material> itemPool) {
        this(itemPool, ItemStack::new);
    }

    RandomizerGameListener(List<Material> itemPool, BiFunction<Material, Integer, ItemStack> itemFactory) {
        mappings = new RandomizerMappings(itemPool);
        this.itemFactory = itemFactory;
    }

    public void saveToFile() {
        WorldStorage.save(FILE_NAME, mappings.save());
    }

    public void loadFromFile() {
        if (!WorldStorage.exists(FILE_NAME)) return;
        RandomizerMappings.SaveData data = WorldStorage.load(FILE_NAME, RandomizerMappings.SaveData.class);
        if (data != null) mappings.load(data);
    }

    public void resetMappings() {
        mappings.clear();
        explosionDrops.clear();
        brokenContainers.clear();
        pendingDrops.clear();
        handledBlocks.clear();
        handledDropItems.clear();
        blockTick = -1;
        explosionTick = -1;
        containerTick = -1;
        WorldStorage.delete(FILE_NAME);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDrop(BlockDropItemEvent event) {
        if (!blockDropsEnabled()) return;
        Location location = event.getBlock().getLocation();
        syncBlockTick(Bukkit.getCurrentTick());
        for (Item item : event.getItems()) {
            handledDropItems.add(item.getUniqueId());
            ItemStack stack = item.getItemStack();
            ItemStack replacement = randomizeBlockDrop(stack, event.getPlayer(), location);
            if (replacement != stack) item.setItemStack(replacement);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!blockDropsEnabled() || !event.isDropItems()) return;
        int tick = Bukkit.getCurrentTick();
        if (containerTick != tick) brokenContainers.clear();
        containerTick = tick;
        BlockState state = event.getBlock().getState();
        if (state instanceof Container) brokenContainers.put(state.getLocation(), containerContents(state));

        Block otherPart = otherPart(event.getBlock(), state.getBlockData());
        if (otherPart != null) addPending(otherPart.getLocation(), event.getPlayer());
    }

    private Block otherPart(Block block, BlockData data) {
        if (data instanceof Bed bed) {
            BlockFace facing = bed.getFacing();
            Block other = block.getRelative(bed.getPart() == Bed.Part.FOOT ? facing : facing.getOppositeFace());
            return other.getBlockData() instanceof Bed opposite && opposite.getMaterial() == bed.getMaterial()
                    && opposite.getPart() != bed.getPart() && opposite.getFacing() == facing ? other : null;
        }
        if (data instanceof Bisected bisected && !(data instanceof Stairs) && !(data instanceof TrapDoor)) {
            Block other = block.getRelative(bisected.getHalf() == Bisected.Half.TOP ? BlockFace.DOWN : BlockFace.UP);
            return other.getBlockData() instanceof Bisected opposite && opposite.getMaterial() == bisected.getMaterial()
                    && opposite.getHalf() != bisected.getHalf() ? other : null;
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreakBlock(BlockBreakBlockEvent event) {
        if (!blockDropsEnabled()) return;
        Location location = event.getBlock().getLocation();
        markBlockHandled(location);
        Player player = pendingDrops.get(BlockPos.of(location));
        event.getDrops().replaceAll(stack -> randomizeBlockDrop(stack, player, location));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        trackExplosion(event.blockList(), responsiblePlayer(event.getEntity()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        trackExplosion(event.blockList(), null);
    }

    private void trackExplosion(List<Block> blocks, Player player) {
        if (!blockDropsEnabled()) return;
        int tick = Bukkit.getCurrentTick();
        if (explosionTick != tick) explosionDrops.clear();
        explosionTick = tick;
        for (Block block : blocks) {
            explosionDrops.put(BlockPos.of(block.getLocation()), new ExplosionDrops(player, containerContents(block.getState())));
        }
    }

    private void syncBlockTick(int tick) {
        if (blockTick != tick) {
            pendingDrops.clear();
            handledBlocks.clear();
            handledDropItems.clear();
            blockTick = tick;
        }
    }

    private void markBlockHandled(Location location) {
        syncBlockTick(Bukkit.getCurrentTick());
        handledBlocks.add(BlockPos.of(location));
    }

    private void addPending(Location location, Player player) {
        syncBlockTick(Bukkit.getCurrentTick());
        BlockPos key = BlockPos.of(location);
        // never replace a known responsible player with "unknown"
        if (player != null) pendingDrops.put(key, player);
        else pendingDrops.putIfAbsent(key, null);
    }

    /**
     * Placing a fluid directly into a replaceable block (flowers, grass, ...) destroys it and spawns the
     * drop without firing any block drop event, so remember the block and randomize on item spawn.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (!blockDropsEnabled()) return;
        addPending(event.getBlock().getLocation(), event.getPlayer());
    }

    /** Dispensers emptying a bucket (water, lava, fish buckets, ...) into a block that then breaks. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDispense(BlockDispenseEvent event) {
        if (!blockDropsEnabled()) return;
        Block block = event.getBlock();
        if (block.getType() != Material.DISPENSER) return;
        Material type = event.getItem().getType();
        if (type == Material.BUCKET || type == Material.MILK_BUCKET || !type.name().endsWith("_BUCKET")) return;
        if (block.getBlockData() instanceof Directional directional) {
            addPending(block.getRelative(directional.getFacing()).getLocation(), null);
        }
    }

    /** Any fluid flowing into a block that is not air/fluid (leaves, plants, ...) may destroy it and drop items. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFluidFlow(BlockFromToEvent event) {
        if (!blockDropsEnabled()) return;
        Block to = event.getToBlock();
        if (!to.getType().isAir() && to.getType() != Material.WATER && to.getType() != Material.LAVA) {
            addPending(to.getLocation(), null);
        }
    }

    /** Fluid level changes in a block (placing/flowing fluid, waterlogging), including through other blocks. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFluidLevelChange(FluidLevelChangeEvent event) {
        if (!blockDropsEnabled()) return;
        Block block = event.getBlock();
        if (!block.getType().isAir()) addPending(block.getLocation(), null);
    }

    /** Physics (torches, plants, ...), entities changing blocks, etc. Drops have no drop event. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockDestroy(BlockDestroyEvent event) {
        if (!blockDropsEnabled() || !event.willDrop()) return;
        addPending(event.getBlock().getLocation(), null);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLeavesDecay(LeavesDecayEvent event) {
        if (!blockDropsEnabled()) return;
        addPending(event.getBlock().getLocation(), null);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        if (explosionDrops.isEmpty() && pendingDrops.isEmpty()) return;
        int tick = Bukkit.getCurrentTick();
        if (explosionTick != tick) explosionDrops.clear();
        syncBlockTick(tick);
        if (!blockDropsEnabled()) return;
        Item item = event.getEntity();
        if (handledDropItems.contains(item.getUniqueId())) return;
        Location location = event.getLocation();
        BlockPos blockLocation = BlockPos.of(location);
        if (handledBlocks.contains(blockLocation)) return;
        Player player;
        if (pendingDrops.containsKey(blockLocation)) {
            player = pendingDrops.get(blockLocation);
        } else {
            ExplosionDrops drops = explosionDrops.get(blockLocation);
            if (drops == null) return;
            player = drops.player();
        }
        ItemStack stack = item.getItemStack();
        ItemStack replacement = randomizeBlockDrop(stack, player, location);
        if (replacement != stack) item.setItemStack(replacement);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerShear(PlayerShearEntityEvent event) {
        if (!mobDropsEnabled()) return;
        List<ItemStack> drops = new ArrayList<>(event.getDrops());
        randomize(drops, event.getPlayer(), event.getEntity().getLocation());
        event.setDrops(drops);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockShear(BlockShearEntityEvent event) {
        if (!mobDropsEnabled()) return;
        List<ItemStack> drops = new ArrayList<>(event.getDrops());
        randomize(drops, null, event.getEntity().getLocation());
        event.setDrops(drops);
    }

    /** Berries / cave vines: same item mapping as breaking the plant, since mappings are per item type. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHarvest(PlayerHarvestBlockEvent event) {
        if (!blockDropsEnabled()) return;
        randomize(event.getItemsHarvested(), event.getPlayer(), event.getHarvestedBlock().getLocation());
    }

    /** Shearing a pumpkin / beehive: same mapping as the item from any other source (chests, etc.). */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onShearBlock(PlayerShearBlockEvent event) {
        if (!blockDropsEnabled()) return;
        randomize(event.getDrops(), event.getPlayer(), event.getBlock().getLocation());
    }

    /**
     * A full composter spawns its bone meal as an item with no event. The item appears up to ~0.7 blocks
     * around the point above the composter, so every block in that area is marked.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onComposterEmpty(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        Block block = event.getClickedBlock();
        if (block.getType() != Material.COMPOSTER || !blockDropsEnabled()) return;
        if (!(block.getBlockData() instanceof Levelled levelled)
                || levelled.getLevel() < levelled.getMaximumLevel()) return;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 0; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    addPending(block.getLocation().add(dx, dy, dz), event.getPlayer());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        if (!ChallengeManager.isGameplayActive(ChallengeType.RANDOMIZER) || !RandomizerSettings.isFishingEnabled()) return;
        if (!(event.getCaught() instanceof Item item)) return;
        List<ItemStack> caught = new ArrayList<>();
        caught.add(item.getItemStack());
        randomize(caught, event.getPlayer(), item.getLocation());
        item.setItemStack(caught.get(0));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPiglinBarter(PiglinBarterEvent event) {
        if (!ChallengeManager.isGameplayActive(ChallengeType.RANDOMIZER) || !RandomizerSettings.isPiglinBarteringEnabled()) return;
        randomize(event.getOutcome(), null, event.getEntity().getLocation());
    }

    private boolean mobDropsEnabled() {
        return ChallengeManager.isGameplayActive(ChallengeType.RANDOMIZER) && RandomizerSettings.isMobDropsEnabled();
    }

    private boolean blockDropsEnabled() {
        return ChallengeManager.isGameplayActive(ChallengeType.RANDOMIZER) && RandomizerSettings.isBlockDropsEnabled();
    }

    private Player responsiblePlayer(Entity entity) {
        if (entity instanceof Player player) return player;
        if (entity instanceof TNTPrimed tnt) return responsiblePlayer(tnt.getSource());
        if (entity instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) {
            return responsiblePlayer(shooter);
        }
        return entity instanceof LivingEntity living ? living.getKiller() : null;
    }

    private List<ItemStack> containerContents(BlockState state) {
        List<ItemStack> contents = new ArrayList<>();
        if (state instanceof Container container) {
            for (ItemStack stack : container.getSnapshotInventory().getContents()) {
                if (stack != null && !stack.isEmpty()) contents.add(stack.clone());
            }
        }
        return contents;
    }

    ItemStack randomizeBlockDrop(ItemStack stack, Player player) {
        return randomizeBlockDrop(stack, player, null);
    }

    ItemStack randomizeBlockDrop(ItemStack stack, Player player, Location location) {
        if (stack == null || stack.isEmpty()) return stack;
        return randomize(stack, resolvePlayer(player, location));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMobDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) return;
        if (!ChallengeManager.isGameplayActive(ChallengeType.RANDOMIZER)) return;
        if (!RandomizerSettings.isMobDropsEnabled()) return;
        randomize(event.getDrops(), event.getEntity().getKiller(), event.getEntity().getLocation());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLootGenerate(LootGenerateEvent event) {
        if (!ChallengeManager.isGameplayActive(ChallengeType.RANDOMIZER)) return;
        Player player = event.getEntity() instanceof Player p ? p : null;
        ExplosionDrops containerDrops = null;
        List<ItemStack> contents = null;
        if (event.getInventoryHolder() instanceof BlockState state) {
            int tick = Bukkit.getCurrentTick();
            if (containerTick == tick) contents = brokenContainers.get(state.getLocation());
            if (explosionTick == tick) {
                containerDrops = explosionDrops.get(BlockPos.of(state.getLocation()));
                if (containerDrops != null) {
                    player = containerDrops.player();
                    contents = containerDrops.contents();
                }
            }
        }
        if (RandomizerSettings.isChestLootEnabled()) {
            randomize(event.getLoot(), player, event.getLootContext().getLocation());
        }
        if (contents != null) {
            for (ItemStack stack : event.getLoot()) {
                if (stack != null && !stack.isEmpty()) contents.add(stack.clone());
            }
        }
    }

    void randomize(List<ItemStack> drops, Player player) {
        randomize(drops, player, null);
    }

    void randomize(List<ItemStack> drops, Player player, Location location) {
        Player owner = resolvePlayer(player, location);
        if (owner != null && !GameEntryService.get().allowsProgress(owner.getUniqueId())) return;
        drops.replaceAll(stack -> randomize(stack, owner));
    }

    private ItemStack randomize(ItemStack stack, Player player) {
        if (stack == null || stack.isEmpty()) return stack;
        Material replacement = mappings.replace(mappingOwner(player), stack.getType(),
                () -> SessionPersistence.requestSave(ChallengeType.RANDOMIZER));
        return itemFactory.apply(replacement, stack.getAmount());
    }

    /**
     * Drops caused by the environment (water, pistons, drowning, fall damage, ...) have no responsible
     * player. In team/per-player mode they must still use a player's mapping, otherwise the same item
     * would randomize differently depending on how it was obtained. Falls back to the nearest
     * participating player in the same world.
     */
    private Player resolvePlayer(Player player, Location location) {
        if (player != null || location == null || location.getWorld() == null) return player;
        if (!usesScopedMappings(RandomizerSettings.isSharedMappingsEnabled(),
                RandomizerSettings.isTeamMappingsEnabled())) return null;
        Player nearest = null;
        double best = Double.MAX_VALUE;
        for (Player candidate : location.getWorld().getPlayers()) {
            if (!GameEntryService.get().allowsProgress(candidate.getUniqueId())) continue;
            double distance = candidate.getLocation().distanceSquared(location);
            if (distance < best) {
                best = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    static boolean usesScopedMappings(boolean sharedMappings, boolean teamMappings) {
        return teamMappings || !sharedMappings;
    }

    private String mappingOwner(Player player) {
        if (player == null) return null;
        return mappingOwner(player, RandomizerSettings.isSharedMappingsEnabled(), RandomizerSettings.isTeamMappingsEnabled());
    }

    static String mappingOwner(Player player, boolean sharedMappings, boolean teamMappings) {
        if (player == null) return null;
        if (teamMappings) {
            KatzeTeam team = KatzePlayer.adapt(player.getUniqueId()).map(KatzePlayer::getTeam).orElse(null);
            if (team != null) return "team:" + team.getId();
        } else if (sharedMappings) {
            return null;
        }
        return "player:" + player.getUniqueId();
    }

    private static List<Material> buildItemPool() {
        List<Material> materials = new ArrayList<>();
        for (Material material : Material.values()) {
            if (material.isItem() && !material.isAir()) materials.add(material);
        }
        Collections.shuffle(materials);
        return materials;
    }
}
