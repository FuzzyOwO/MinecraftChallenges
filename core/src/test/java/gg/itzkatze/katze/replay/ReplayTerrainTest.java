package gg.itzkatze.katze.replay;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.nio.file.attribute.FileTime;

import static org.junit.jupiter.api.Assertions.*;

class ReplayTerrainTest {
    @TempDir Path directory;

    @Test void unchangedChunksShareTheBaselineInsteadOfDuplicatingItInResumeState() throws Exception {
        UUID world = UUID.randomUUID();
        Path baseline = directory.resolve("chunks"), state = directory.resolve("state");
        var terrain = new ReplayTerrain(state, baseline, false);
        for (int x = 0; x < 80; x++) {
            var key = new ReplayStore.ChunkKey(world, x, 0);
            var chunk = air();
            assertNull(terrain.replace(key, chunk));
            chunk.write(baseline.resolve(key.fileName()));
        }
        terrain.flush();
        assertFalse(Files.exists(state));
        var changed = new ReplayStore.ChunkKey(world, 0, 0);
        terrain.get(changed).update(world, 0, -64, 0, "minecraft:stone");
        terrain.changed(changed);
        terrain.flush();
        try (var files = Files.list(state)) { assertEquals(1, files.count()); }
        var resumed = new ReplayTerrain(state, baseline, true);
        assertEquals("minecraft:stone", resumed.get(changed).update(world, 0, -64, 0, "minecraft:air").before());
        var unchanged = new ReplayStore.ChunkKey(world, 1, 0);
        assertEquals("minecraft:air", resumed.get(unchanged).update(world, 16, -64, 0, "minecraft:dirt").before());
    }

    @Test void evictedTerrainRetainsBlockChangesAndCanResumeAfterRestart() throws Exception {
        UUID world = UUID.randomUUID();
        var terrain = new ReplayTerrain(directory, false);
        var first = new ReplayStore.ChunkKey(world, -1, 0);
        assertNull(terrain.replace(first, air()));
        terrain.get(first).update(world, -1, -64, 0, "minecraft:stone");
        terrain.changed(first);
        for (int x = 0; x < 80; x++) terrain.replace(new ReplayStore.ChunkKey(world, x, 0), air());
        assertEquals("minecraft:stone", terrain.get(first).update(world, -1, -64, 0, "minecraft:dirt").before());
        terrain.changed(first);
        terrain.flush();
        terrain.clear();
        var resumed = new ReplayTerrain(directory, true);
        assertEquals("minecraft:dirt", resumed.get(first).update(world, -1, -64, 0, "minecraft:air").before());
        assertNull(resumed.get(new ReplayStore.ChunkKey(world, 1000, 0)));
    }

    @Test void playbackReadsChunksOnlyOnDemandAndEvictsOldDecodedChunks() throws Exception {
        UUID world = UUID.randomUUID();
        for (int x = 0; x < 80; x++) air().write(directory.resolve(new ReplayStore.ChunkKey(world, x, 0).fileName()));
        var chunks = new ReplayChunkFiles(directory);
        var key = new ReplayStore.ChunkKey(world, 0, 0);
        var first = chunks.get(key);
        assertSame(first, chunks.get(key));
        for (int x = 1; x < 80; x++) assertNotNull(chunks.get(new ReplayStore.ChunkKey(world, x, 0)));
        assertNotSame(first, chunks.get(key));
        assertEquals(80, chunks.size());
        assertNull(chunks.get(new ReplayStore.ChunkKey(world, -1, 0)));

        var broken = new ReplayStore.ChunkKey(world, 100, 0);
        Files.writeString(directory.resolve(broken.fileName()), "broken");
        var lazy = new ReplayChunkFiles(directory);
        assertNotNull(lazy.get(key));
        assertThrows(UncheckedIOException.class, () -> lazy.get(broken));
    }

    @Test void unchangedTerrainDoesNotRewriteStateWhenItsCacheEntryIsEvicted() throws Exception {
        UUID world = UUID.randomUUID();
        var terrain = new ReplayTerrain(directory, false);
        var first = new ReplayStore.ChunkKey(world, 0, 0);
        terrain.replace(first, air());
        terrain.flush();
        Path file = directory.resolve(first.fileName());
        FileTime original = FileTime.fromMillis(1_000_000);
        Files.setLastModifiedTime(file, original);
        terrain.replace(first, air());
        for (int x = 1; x < 80; x++) terrain.replace(new ReplayStore.ChunkKey(world, x, 0), air());
        assertEquals(original, Files.getLastModifiedTime(file));
        assertNotNull(terrain.get(first));
    }

    private ReplayChunk air() { return new ReplayChunk(-64, -63, List.of("minecraft:air"), new short[256]); }
}
