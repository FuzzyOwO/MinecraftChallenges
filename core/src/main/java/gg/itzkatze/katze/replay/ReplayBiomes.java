package gg.itzkatze.katze.replay;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class ReplayBiomes extends BiomeProvider {
    private final UUID world;
    private final Map<ReplayStore.ChunkKey, ReplayChunk> chunks;
    private final Biome fallback;
    private final Map<String, Biome> palette = new ConcurrentHashMap<>();
    private final Registry<Biome> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME);

    ReplayBiomes(UUID world, Map<ReplayStore.ChunkKey, ReplayChunk> chunks, World.Environment environment) {
        this.world = world;
        this.chunks = chunks;
        fallback = switch (environment) {
            case NETHER -> Biome.NETHER_WASTES;
            case THE_END -> Biome.THE_END;
            default -> Biome.PLAINS;
        };
    }

    @Override public @NotNull Biome getBiome(@NotNull WorldInfo info, int x, int y, int z) {
        ReplayChunk chunk = chunks.get(new ReplayStore.ChunkKey(world, x >> 4, z >> 4));
        String key = chunk == null ? null : chunk.biome(x, y, z);
        if (key == null) return fallback;
        return palette.computeIfAbsent(key, value -> {
            NamespacedKey name = NamespacedKey.fromString(value);
            Biome biome = name == null ? null : registry.get(name);
            return biome == null ? fallback : biome;
        });
    }

    @Override public @NotNull List<Biome> getBiomes(@NotNull WorldInfo info) { return registry.stream().toList(); }
}
