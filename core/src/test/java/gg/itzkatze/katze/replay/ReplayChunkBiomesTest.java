package gg.itzkatze.katze.replay;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ReplayChunkBiomesTest {
    @TempDir Path directory;

    @Test void biomePaletteRoundTripPreservesCavesSurfaceAndNegativeCoordinates() throws Exception {
        short[] biomes = new short[32];
        biomes[0] = 1;
        biomes[31] = 2;
        var chunk = new ReplayChunk(-64, -56, List.of("minecraft:air"), new short[8 * 256],
                List.of("minecraft:plains", "minecraft:lush_caves", "custom:forest"), biomes);
        Path path = directory.resolve("chunk.gz");
        chunk.write(path);
        var saved = ReplayChunk.read(path);
        assertEquals("minecraft:lush_caves", saved.biome(-16, -64, -16));
        assertEquals("minecraft:lush_caves", saved.biome(-13, -61, -13));
        assertEquals("minecraft:plains", saved.biome(-12, -64, -16));
        assertEquals("custom:forest", saved.biome(-1, -57, -1));
        assertEquals("custom:forest", saved.biome(15, 1000, 15));
    }

    @Test void preBiomeChunksStillLoadAndDoNotInventBiomeData() throws Exception {
        Path path = directory.resolve("old.gz");
        new ReplayChunk(-64, -60, List.of("minecraft:air"), new short[4 * 256]).write(path);
        assertNull(ReplayChunk.read(path).biome(0, -64, 0));
    }

    @Test void invalidBiomeRunsAndTruncatedBiomeExtensionsAreRejected() throws Exception {
        for (int mode = 0; mode < 3; mode++) {
            Path path = directory.resolve("broken" + mode + ".gz");
            try (var out = new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(path)))) {
                out.writeInt(-64); out.writeInt(-60); out.writeInt(1); out.writeUTF("minecraft:air");
                out.writeShort(0); out.writeInt(4 * 256);
                if (mode == 2) { out.writeByte(0x42); continue; }
                out.writeInt(0x42494f31); out.writeInt(1); out.writeUTF("minecraft:plains");
                out.writeShort(mode == 0 ? 1 : 0); out.writeInt(mode == 0 ? 16 : 17);
            }
            assertThrows(IOException.class, () -> ReplayChunk.read(path));
        }
    }
}
