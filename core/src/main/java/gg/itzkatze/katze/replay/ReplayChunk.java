package gg.itzkatze.katze.replay;

import org.bukkit.ChunkSnapshot;
import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;
import org.bukkit.generator.ChunkGenerator.ChunkData;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class ReplayChunk {
    private static final int BIOME_FORMAT = 0x42494f31;
    private final int minY;
    private final int maxY;
    private final List<String> palette;
    private final short[] blocks;
    private final List<String> biomePalette;
    private final short[] biomes;

    @FunctionalInterface
    interface BiomeLookup { String get(int x, int y, int z); }

    ReplayChunk(int minY, int maxY, List<String> palette, short[] blocks) {
        this(minY, maxY, palette, blocks, List.of(), new short[0]);
    }

    ReplayChunk(int minY, int maxY, List<String> palette, short[] blocks, List<String> biomePalette, short[] biomes) {
        this.minY = minY;
        this.maxY = maxY;
        this.palette = new ArrayList<>(palette);
        this.blocks = blocks;
        this.biomePalette = List.copyOf(biomePalette);
        this.biomes = biomes;
    }

    static ReplayChunk capture(ChunkSnapshot snapshot, int minY, int maxY) {
        return capture(snapshot, minY, maxY, new LinkedHashMap<>());
    }

    static ReplayChunk capture(ChunkSnapshot snapshot, int minY, int maxY, Map<BlockData, String> blockStates) {
        return capture(snapshot, minY, maxY, blockStates, (x, y, z) -> snapshot.getBiome(x, y, z).getKey().asString());
    }

    static ReplayChunk capture(ChunkSnapshot snapshot, int minY, int maxY, Map<BlockData, String> blockStates, BiomeLookup biomeLookup) {
        Map<String, Integer> palette = new LinkedHashMap<>();
        short[] blocks = new short[(maxY - minY) * 256];
        for (int y = minY; y < maxY; y++) {
            if (snapshot.isSectionEmpty((y - minY) >> 4)) {
                short air = palette.computeIfAbsent("minecraft:air", ignored -> palette.size()).shortValue();
                Arrays.fill(blocks, (y - minY) * 256, (y - minY + 1) * 256, air);
                continue;
            }
            for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                String block = blockStates.computeIfAbsent(snapshot.getBlockData(x, y, z), BlockData::getAsString);
                blocks[(y - minY) * 256 + z * 16 + x] = palette.computeIfAbsent(block, ignored -> palette.size()).shortValue();
            }
        }
        Map<String, Integer> biomePalette = new LinkedHashMap<>();
        short[] biomes = new short[biomeCells(minY, maxY)];
        for (int y = minY >> 2; y <= ((maxY - 1) >> 2); y++) for (int z = 0; z < 4; z++) for (int x = 0; x < 4; x++) {
            String biome = biomeLookup.get(x << 2, Math.max(minY, y << 2), z << 2);
            biomes[(y - (minY >> 2)) * 16 + z * 4 + x] = biomePalette.computeIfAbsent(biome, ignored -> biomePalette.size()).shortValue();
        }
        return new ReplayChunk(minY, maxY, List.copyOf(palette.keySet()), blocks, List.copyOf(biomePalette.keySet()), biomes);
    }

    List<ReplayFrame.BlockChange> changes(UUID world, int chunkX, int chunkZ, ReplayChunk previous) {
        if (sameBlocks(previous)) return List.of();
        List<ReplayFrame.BlockChange> changes = new ArrayList<>();
        for (int i = 0; i < blocks.length; i++) {
            String after = block(i);
            String before = previous.block(i);
            if (!before.equals(after)) changes.add(new ReplayFrame.BlockChange(world,
                    chunkX * 16 + i % 16, minY + i / 256, chunkZ * 16 + i / 16 % 16, before, after));
        }
        return changes;
    }

    boolean sameContents(ReplayChunk other) {
        if (!sameBlocks(other) || biomes.length != other.biomes.length) return false;
        if (biomePalette.equals(other.biomePalette)) return Arrays.equals(biomes, other.biomes);
        for (int i = 0; i < biomes.length; i++) {
            if (!biomePalette.get(Short.toUnsignedInt(biomes[i])).equals(other.biomePalette.get(Short.toUnsignedInt(other.biomes[i])))) return false;
        }
        return true;
    }

    private boolean sameBlocks(ReplayChunk other) {
        if (minY != other.minY || maxY != other.maxY) return false;
        if (palette.equals(other.palette)) return Arrays.equals(blocks, other.blocks);
        for (int i = 0; i < blocks.length; i++) if (!block(i).equals(other.block(i))) return false;
        return true;
    }

    private String block(int index) {
        return palette.get(Short.toUnsignedInt(blocks[index]));
    }

    ReplayFrame.BlockChange update(UUID world, int x, int y, int z, String after) {
        if (y < minY || y >= maxY) return null;
        int index = (y - minY) * 256 + (z & 15) * 16 + (x & 15);
        String before = block(index);
        if (before.equals(after)) return null;
        int material = palette.indexOf(after);
        if (material < 0) {
            material = palette.size();
            if (material >= 65536) throw new IllegalStateException("Replay chunk palette is full");
            palette.add(after);
        }
        blocks[index] = (short) material;
        return new ReplayFrame.BlockChange(world, x, y, z, before, after);
    }

    void generate(ChunkData data, Map<String, BlockData> blockData) {
        for (int y = minY; y < maxY; y++) for (int z = 0; z < 16; z++) {
            int x = 0;
            while (x < 16) {
                String material = block((y - minY) * 256 + z * 16 + x);
                int end = x + 1;
                while (end < 16 && block((y - minY) * 256 + z * 16 + end).equals(material)) end++;
                if (!material.equals("minecraft:air") && !material.equals("minecraft:cave_air") && !material.equals("minecraft:void_air")) {
                    data.setRegion(x, y, z, end, y + 1, z + 1, blockData.computeIfAbsent(material, Bukkit::createBlockData));
                }
                x = end;
            }
        }
    }

    List<String> palette() { return palette; }

    String biome(int x, int y, int z) {
        if (biomes.length == 0) return null;
        int layer = Math.clamp(y >> 2, minY >> 2, (maxY - 1) >> 2) - (minY >> 2);
        return biomePalette.get(Short.toUnsignedInt(biomes[layer * 16 + (z & 15) / 4 * 4 + (x & 15) / 4]));
    }

    private static int biomeCells(int minY, int maxY) { return (((maxY - 1) >> 2) - (minY >> 2) + 1) * 16; }

    void write(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        try (var out = new DataOutputStream(ReplayCompression.write(Files.newOutputStream(path)))) {
            out.writeInt(minY);
            out.writeInt(maxY);
            out.writeInt(palette.size());
            for (String block : palette) out.writeUTF(block);
            for (int i = 0; i < blocks.length;) {
                int end = i + 1;
                while (end < blocks.length && blocks[end] == blocks[i]) end++;
                out.writeShort(blocks[i]);
                out.writeInt(end - i);
                i = end;
            }
            if (!biomePalette.isEmpty()) {
                out.writeInt(BIOME_FORMAT);
                out.writeInt(biomePalette.size());
                for (String biome : biomePalette) out.writeUTF(biome);
                for (int i = 0; i < biomes.length;) {
                    int end = i + 1;
                    while (end < biomes.length && biomes[end] == biomes[i]) end++;
                    out.writeShort(biomes[i]);
                    out.writeInt(end - i);
                    i = end;
                }
            }
        }
    }

    static ReplayChunk read(Path path) throws IOException {
        try (var in = new DataInputStream(ReplayCompression.read(Files.newInputStream(path)))) {
            int minY = in.readInt(), maxY = in.readInt(), size = in.readInt();
            if (minY < -2032 || maxY > 2032 || minY >= maxY || size < 1 || size > 65536) throw new IOException("Invalid replay chunk");
            List<String> palette = new ArrayList<>();
            for (int i = 0; i < size; i++) palette.add(in.readUTF());
            short[] blocks = new short[(maxY - minY) * 256];
            for (int i = 0; i < blocks.length;) {
                short block = in.readShort();
                int count = in.readInt();
                if (Short.toUnsignedInt(block) >= size || count < 1 || count > blocks.length - i) throw new IOException("Invalid replay chunk run");
                Arrays.fill(blocks, i, i + count, block);
                i += count;
            }
            int first = in.read();
            if (first == -1) return new ReplayChunk(minY, maxY, palette, blocks);
            int format = first << 24 | in.readUnsignedByte() << 16 | in.readUnsignedByte() << 8 | in.readUnsignedByte();
            int biomeCount = in.readInt();
            if (format != BIOME_FORMAT || biomeCount < 1 || biomeCount > 65536) throw new IOException("Invalid replay biomes");
            List<String> biomePalette = new ArrayList<>();
            for (int i = 0; i < biomeCount; i++) biomePalette.add(in.readUTF());
            short[] biomes = new short[biomeCells(minY, maxY)];
            for (int i = 0; i < biomes.length;) {
                short biome = in.readShort();
                int count = in.readInt();
                if (Short.toUnsignedInt(biome) >= biomeCount || count < 1 || count > biomes.length - i) throw new IOException("Invalid replay biome run");
                Arrays.fill(biomes, i, i + count, biome);
                i += count;
            }
            if (in.read() != -1) throw new IOException("Unexpected replay chunk data");
            return new ReplayChunk(minY, maxY, palette, blocks, biomePalette, biomes);
        }
    }
}
