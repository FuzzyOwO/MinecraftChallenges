package gg.itzkatze.katze.replay;

import gg.itzkatze.katze.game.GameWorldDirectory;
import io.papermc.paper.math.Position;
import org.bukkit.Bukkit;
import org.bukkit.GameRules;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.data.BlockData;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class ReplayWorlds implements AutoCloseable {
    private final Map<UUID, World> worlds = new HashMap<>();

    ReplayWorlds(ReplayStore.Loaded replay) {
        UUID session = UUID.randomUUID();
        Map<String, BlockData> palette = new ConcurrentHashMap<>();
        Map<UUID, ReplayFrame.Scene> scenes = new HashMap<>();
        replay.scenes().forEach(scene -> scenes.putIfAbsent(scene.world(), scene));
        try {
            for (ReplayFrame.Scene scene : scenes.values()) {
                Map<ReplayStore.ChunkKey, ReplayChunk> chunks = replay.chunks();
                ChunkGenerator generator = new ChunkGenerator() {
                    @Override
                    public void generateNoise(@NotNull WorldInfo info, @NotNull Random random, int x, int z, @NotNull ChunkData data) {
                        ReplayChunk chunk = chunks.get(new ReplayStore.ChunkKey(scene.world(), x, z));
                        if (chunk != null) chunk.generate(data, palette);
                    }
                };
                NamespacedKey key = new NamespacedKey("katzegames", "replay_" + session + "_" + scene.world());
                World.Environment environment = World.Environment.valueOf(scene.environment());
                World world = Objects.requireNonNull(WorldCreator.ofKey(key).environment(environment).biomeProvider(new ReplayBiomes(scene.world(), chunks, environment))
                        .generator(generator).generateStructures(false).forcedSpawnPosition(Position.block(0, 64, 0), 0, 0).createWorld(), "Failed to create replay world");
                worlds.put(scene.world(), world);
                world.setAutoSave(false);
                world.setGameRule(GameRules.SPAWN_MOBS, false);
                world.setGameRule(GameRules.SPAWN_MONSTERS, false);
                world.setGameRule(GameRules.ADVANCE_TIME, false);
                world.setGameRule(GameRules.ADVANCE_WEATHER, false);
                world.setGameRule(GameRules.RANDOM_TICK_SPEED, 0);
                world.setGameRule(GameRules.MOB_GRIEFING, false);
                world.setGameRule(GameRules.SPAWNER_BLOCKS_WORK, false);
                world.setGameRule(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, 0);
                world.setGameRule(GameRules.LOCATOR_BAR, false);
                applyScene(world, scene);
            }
        } catch (RuntimeException e) {
            try { close(); } catch (IOException cleanup) { e.addSuppressed(cleanup); }
            throw e;
        }
    }

    World get(UUID source) { return Objects.requireNonNull(worlds.get(source), "Missing replay dimension"); }
    boolean contains(World world) { return worlds.containsValue(world); }

    static void applyScene(World world, ReplayFrame.Scene scene) {
        if (world.getEnvironment() == World.Environment.NORMAL && !world.isFixedTime() && world.getTime() != scene.time()) world.setTime(scene.time());
        if (world.hasStorm() != scene.storm()) world.setStorm(scene.storm());
        if (world.isThundering() != scene.thunder()) world.setThundering(scene.thunder());
    }

    @Override
    public void close() throws IOException {
        IOException failure = null;
        for (World world : worlds.values()) {
            try {
                GameWorldDirectory directory = new GameWorldDirectory(Bukkit.getServer(), world.getKey());
                directory.validateWorldPath(world.getWorldPath());
                if (!world.getPlayers().isEmpty() || !Bukkit.unloadWorld(world, false)) throw new IOException("Replay world still in use: " + world.getKey());
                directory.delete();
            } catch (IOException e) {
                if (failure == null) failure = e;
                else failure.addSuppressed(e);
            }
        }
        worlds.clear();
        if (failure != null) throw failure;
    }
}
