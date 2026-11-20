package gg.itzkatze.katze.replay;

import org.bukkit.ChunkSnapshot;
import org.bukkit.block.data.BlockData;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class ReplayChunkCaptureTest {
    @Test
    void sectionIndexesAreRelativeWhileBlockCoordinatesKeepTheirWorldHeight() {
        for (int minY : List.of(-64, 0, 64)) {
            int maxY = minY + 384;
            boolean[] sections = new boolean[24];
            Arrays.fill(sections, true);
            sections[0] = false;
            sections[23] = false;
            BlockData stone = block("minecraft:stone"), air = block("minecraft:air");
            ChunkSnapshot snapshot = (ChunkSnapshot) Proxy.newProxyInstance(ChunkSnapshot.class.getClassLoader(), new Class<?>[]{ChunkSnapshot.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "isSectionEmpty" -> sections[(int) args[0]];
                        case "getBlockData" -> ((int) args[0] == 0 && (int) args[1] == minY && (int) args[2] == 0)
                                || ((int) args[0] == 15 && (int) args[1] == maxY - 1 && (int) args[2] == 15) ? stone : air;
                        default -> throw new AssertionError(method.getName());
                    });
            ReplayChunk captured = ReplayChunk.capture(snapshot, minY, maxY, new HashMap<>(), (x, y, z) -> {
                assertTrue(x >= 0 && x < 16 && z >= 0 && z < 16);
                assertTrue(y >= minY && y < maxY);
                assertEquals(0, x % 4);
                assertEquals(0, z % 4);
                return y < minY + 4 ? "minecraft:lush_caves" : "minecraft:plains";
            });
            ReplayChunk empty = new ReplayChunk(minY, maxY, List.of("minecraft:air"), new short[384 * 256]);
            UUID world = UUID.randomUUID();
            assertEquals(List.of(
                    new ReplayFrame.BlockChange(world, -16, minY, -16, "minecraft:air", "minecraft:stone"),
                    new ReplayFrame.BlockChange(world, -1, maxY - 1, -1, "minecraft:air", "minecraft:stone")), captured.changes(world, -1, -1, empty));
            assertEquals("minecraft:lush_caves", captured.biome(0, minY, 0));
            assertEquals("minecraft:plains", captured.biome(15, maxY - 1, 15));
        }
    }

    private BlockData block(String material) {
        return (BlockData) Proxy.newProxyInstance(BlockData.class.getClassLoader(), new Class<?>[]{BlockData.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getAsString" -> material;
            case "hashCode" -> material.hashCode();
            case "equals" -> proxy == args[0];
            default -> throw new AssertionError(method.getName());
        });
    }

}
