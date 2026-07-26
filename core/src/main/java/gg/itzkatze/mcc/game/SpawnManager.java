package gg.itzkatze.mcc.game;
import gg.itzkatze.mcc.util.StructureUtil;
import lombok.Getter;
import org.bukkit.Bukkit;
import java.io.IOException;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.BoundingBox;

public class SpawnManager implements Listener {

    @Getter
    private static boolean existsSpawn = false;

    @Getter
    private static final Location spawn = new Location(Bukkit.getWorld("world"), 0.5, 201, 0.5);
    private static final BoundingBox spawnBoundingBox = new BoundingBox(-28, 192, -21, 19, 231, 26);
    private static final BoundingBox spawnBarriers = new BoundingBox(-7, 198, 7, 7, 200, -7);

    public static void createSpawnIfNeeded() {
        World world = Bukkit.getWorld("world");
        if (!existsSpawn && world != null) {
            StructureUtil.createSpawn();
            existsSpawn = true;

            world.setTime(6000);
            world.setStorm(false);
            world.setGameRule(GameRules.RANDOM_TICK_SPEED, 0);
            world.setGameRule(GameRules.ADVANCE_TIME, false);
            world.setGameRule(GameRules.ADVANCE_WEATHER, false);
        }
    }

    public static void destroySpawn() {
        existsSpawn = false;
        World world = Bukkit.getWorld("world");
        if (world == null) return;

        // Reset world data
        setWorldData("world");
        setWorldData("world_nether");
        setWorldData("world_the_end");

        // Delete spawn structure
        try {
            Bukkit.getStructureManager().deleteStructure(new NamespacedKey("forcebattle", "spawn"), true);
        } catch (IOException e) {
            // Structure deletion failed, but this is not critical
        }

        // Reset game rules
        world.setGameRule(GameRules.RANDOM_TICK_SPEED, 20);
        world.setGameRule(GameRules.ADVANCE_TIME, true);
        world.setGameRule(GameRules.ADVANCE_WEATHER, true);

        int minX = (int) Math.floor(spawnBoundingBox.getMinX());
        int minY = (int) Math.floor(spawnBoundingBox.getMinY());
        int minZ = (int) Math.floor(spawnBoundingBox.getMinZ());
        int maxX = (int) Math.ceil(spawnBoundingBox.getMaxX());
        int maxY = (int) Math.ceil(spawnBoundingBox.getMaxY());
        int maxZ = (int) Math.ceil(spawnBoundingBox.getMaxZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    world.getBlockAt(x, y, z).setType(Material.AIR, false);
                }
            }
        }
    }

    @EventHandler
    public void onBlockPhysics(BlockPhysicsEvent event) {
        if (!existsSpawn) return;
        if (spawnBoundingBox.contains(event.getBlock().getBoundingBox())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!existsSpawn) return;
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        if (spawnBoundingBox.contains(clickedBlock.getBoundingBox())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!existsSpawn) return;
        Location blockLocation = event.getPlayer().getLocation().subtract(0, 1, 0);
        if (spawnBarriers.contains(blockLocation.getX(), blockLocation.getY(), blockLocation.getZ())) return;

        if (blockLocation.getBlock().getType() == Material.BARRIER) {
            event.getPlayer().teleport(spawn);
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!existsSpawn) return;
        if (event.getEntity().getType() == EntityType.PLAYER) event.setCancelled(true);
    }

    @EventHandler
    public void onHungerChange(FoodLevelChangeEvent event) {
        if (!existsSpawn) return;
        event.setCancelled(true);
    }

    private static void setWorldData(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            // Nether and End dimensions do not have a world clock in modern Paper.
            if (world.getEnvironment() == World.Environment.NORMAL) {
                world.setTime(0);
            }
            world.setStorm(false);
            world.setThundering(false);
            world.setWeatherDuration(0);
            world.setDifficulty(org.bukkit.Difficulty.NORMAL);
            world.setGameRule(GameRules.LOCATOR_BAR, false);
        }
    }
}
