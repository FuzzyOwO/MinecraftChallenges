package gg.itzkatze.mcc.lobby;

import gg.itzkatze.mcc.MinecraftChallenges;
import gg.itzkatze.mcc.player.MccPlayer;
import gg.itzkatze.mcc.util.StructureUtil;
import gg.itzkatze.mcc.util.WorldStorage;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.structure.Mirror;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.structure.Structure;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class LobbyManager implements Listener {

    private static final Logger logger = LoggerFactory.getLogger("MinecraftChallenges/" + LobbyManager.class.getSimpleName());
    private static final String ISLAND_PACKAGE = "gg.itzkatze.mcc.lobby.islands";
    private static final String WORLD_NAME = "world";
    private static final String RECORD_FILE = "lobby.json";
    private static final int BASE_Y = 192;

    private static final List<LobbyIsland> islands = new ArrayList<>();
    private static @Nullable Lobby activeLobby;
    private static @Nullable String lastIslandId;

    public static void loadIslands() {
        islands.clear();
        MinecraftChallenges.loopThroughPackage(ISLAND_PACKAGE, LobbyIsland.class)
                .sorted(Comparator.comparing(LobbyIsland::getId))
                .forEach(islands::add);
        if (islands.isEmpty()) {
            logger.error("No lobby islands found in {}, players will stay where they are.", ISLAND_PACKAGE);
        }
    }

    public static @NotNull List<LobbyIsland> getIslands() {
        return List.copyOf(islands);
    }

    public static boolean isActive() {
        return activeLobby != null;
    }

    public static @Nullable Lobby getActiveLobby() {
        return activeLobby;
    }

    public static @Nullable Location getSpawn() {
        return activeLobby == null ? null : activeLobby.getSpawn().clone();
    }

    public static void createLobbyIfNeeded() {
        if (activeLobby != null) return;
        World world = Bukkit.getWorld(WORLD_NAME);
        if (world == null) return;

        LobbyRecord record = WorldStorage.load(RECORD_FILE, LobbyRecord.class);
        if (record != null) {
            LobbyIsland recorded = findIsland(record.island);
            if (recorded != null && tryCreate(recorded, world)) return;
            clear(record);
            WorldStorage.delete(RECORD_FILE);
        }

        for (LobbyIsland island : selectionOrder(islands, lastIslandId, ThreadLocalRandom.current())) {
            if (tryCreate(island, world)) return;
        }
        logger.error("No lobby island could be pasted.");
    }

    public static void destroyLobby() {
        Lobby lobby = activeLobby;
        activeLobby = null;
        if (lobby != null) {
            for (LobbyFeature feature : lobby.getFeatures()) {
                if (feature instanceof Listener listener) {
                    HandlerList.unregisterAll(listener);
                }
                feature.onDestroy(lobby);
            }
            if (lobby.getWorld() != null) {
                clear(lobby.getWorld(), lobby.getBounds());
            }
        } else {
            LobbyRecord record = WorldStorage.load(RECORD_FILE, LobbyRecord.class);
            if (record != null) {
                clear(record);
            }
        }
        WorldStorage.delete(RECORD_FILE);

        World world = Bukkit.getWorld(WORLD_NAME);
        if (world != null) {
            world.setGameRule(GameRules.RANDOM_TICK_SPEED, 20);
            world.setGameRule(GameRules.ADVANCE_TIME, true);
            world.setGameRule(GameRules.ADVANCE_WEATHER, true);
        }
    }

    public static void onPlayerJoin(@NotNull MccPlayer player) {
        Lobby lobby = activeLobby;
        if (lobby == null) return;
        lobby.getFeatures().forEach(feature -> feature.onPlayerJoin(player, lobby));
    }

    static @NotNull List<LobbyIsland> selectionOrder(@NotNull List<LobbyIsland> candidates, @Nullable String lastId, @NotNull Random random) {
        List<LobbyIsland> order = new ArrayList<>();
        LobbyIsland previous = null;
        for (LobbyIsland island : candidates) {
            if (candidates.size() > 1 && island.getId().equals(lastId)) {
                previous = island;
            } else {
                order.add(island);
            }
        }
        Collections.shuffle(order, random);
        if (previous != null) {
            order.add(previous);
        }
        return order;
    }

    static boolean isFallingOut(@NotNull BoundingBox bounds, double x, double y, double z) {
        return x >= bounds.getMinX() && x < bounds.getMaxX()
                && z >= bounds.getMinZ() && z < bounds.getMaxZ()
                && y <= bounds.getMinY() + 1;
    }

    private static @Nullable LobbyIsland findIsland(@Nullable String id) {
        return islands.stream().filter(island -> island.getId().equals(id)).findFirst().orElse(null);
    }

    private static boolean tryCreate(@NotNull LobbyIsland island, @NotNull World world) {
        Structure structure = StructureUtil.loadStructure(island.getStructureName());
        if (structure == null) return false;

        Lobby lobby = new Lobby(island, world, structure.getSize(), BASE_Y);
        structure.place(lobby.toWorld(new Vector()), true, island.getRotation(), Mirror.NONE, 0, 1.0f, new Random());

        world.setTime(6000);
        world.setStorm(false);
        world.setGameRule(GameRules.RANDOM_TICK_SPEED, 0);
        world.setGameRule(GameRules.ADVANCE_TIME, false);
        world.setGameRule(GameRules.ADVANCE_WEATHER, false);

        WorldStorage.save(RECORD_FILE, LobbyRecord.of(lobby, world));
        activeLobby = lobby;
        lastIslandId = island.getId();

        for (LobbyFeature feature : lobby.getFeatures()) {
            feature.onCreate(lobby);
            if (feature instanceof Listener listener) {
                Bukkit.getPluginManager().registerEvents(listener, MinecraftChallenges.get());
            }
        }
        return true;
    }

    private static void clear(@NotNull LobbyRecord record) {
        World world = record.world == null ? null : Bukkit.getWorld(record.world);
        if (world == null || record.min == null || record.max == null || record.min.length != 3 || record.max.length != 3) return;
        clear(world, BoundingBox.of(new Vector(record.min[0], record.min[1], record.min[2]), new Vector(record.max[0], record.max[1], record.max[2])));
    }

    private static void clear(@NotNull World world, @NotNull BoundingBox bounds) {
        for (int x = (int) bounds.getMinX(); x < (int) bounds.getMaxX(); x++) {
            for (int y = (int) bounds.getMinY(); y < (int) bounds.getMaxY(); y++) {
                for (int z = (int) bounds.getMinZ(); z < (int) bounds.getMaxZ(); z++) {
                    world.getBlockAt(x, y, z).setType(Material.AIR, false);
                }
            }
        }
    }

    private static boolean isInLobby(@NotNull Block block) {
        Lobby lobby = activeLobby;
        return lobby != null && block.getWorld().equals(lobby.getWorld())
                && lobby.getBounds().contains(block.getX() + 0.5, block.getY() + 0.5, block.getZ() + 0.5);
    }

    @EventHandler
    public void onBlockPhysics(BlockPhysicsEvent event) {
        if (isInLobby(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock != null && isInLobby(clickedBlock)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Lobby lobby = activeLobby;
        if (lobby == null) return;

        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.ADVENTURE && player.getGameMode() != GameMode.SURVIVAL) return;

        Location to = event.getTo();
        if (to.getWorld() != null && to.getWorld().equals(lobby.getWorld()) && isFallingOut(lobby.getBounds(), to.getX(), to.getY(), to.getZ())) {
            player.teleport(lobby.getSpawn());
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (activeLobby != null && event.getEntity().getType() == EntityType.PLAYER) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onHungerChange(FoodLevelChangeEvent event) {
        if (activeLobby != null) {
            event.setCancelled(true);
        }
    }

    private static final class LobbyRecord {
        private String island;
        private String world;
        private int[] min;
        private int[] max;

        private static @NotNull LobbyRecord of(@NotNull Lobby lobby, @NotNull World world) {
            LobbyRecord record = new LobbyRecord();
            record.island = lobby.getIsland().getId();
            record.world = world.getName();
            BoundingBox bounds = lobby.getBounds();
            record.min = new int[]{(int) bounds.getMinX(), (int) bounds.getMinY(), (int) bounds.getMinZ()};
            record.max = new int[]{(int) bounds.getMaxX(), (int) bounds.getMaxY(), (int) bounds.getMaxZ()};
            return record;
        }
    }
}
