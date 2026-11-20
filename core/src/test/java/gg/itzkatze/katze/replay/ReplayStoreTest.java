package gg.itzkatze.katze.replay;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class ReplayStoreTest {
    @TempDir Path directory;

    @Test
    void loadsLeadingEmptyFramesAndPreservesTerrainBeforeTheFirstParticipant() throws Exception {
        UUID round = UUID.randomUUID(), player = UUID.randomUUID(), world = UUID.randomUUID();
        ReplayStore store = new ReplayStore(directory);
        var scene = new ReplayFrame.Scene(world, "NORMAL", 1000, false, false);
        var change = new ReplayFrame.BlockChange(world, 0, 64, 0, "minecraft:air", "minecraft:stone");
        var actor = new ReplayFrame.Actor(player, world, "PLAYER", "Katze", null, "skin", "signature",
                0, 64, 0, 0, 0, "STANDING", false, false, List.of());
        var initial = new ReplayFrame(0, List.of(), List.of(change), List.of(scene));
        var active = new ReplayFrame(2, List.of(actor), List.of(), List.of(scene));
        try (var writer = store.writer(round, false)) {
            store.writeFrame(writer, initial);
            store.writeFrame(writer, active);
        }
        store.writeChunk(round, new ReplayStore.ChunkKey(world, 0, 0), new ReplayChunk(64, 65, List.of("minecraft:air"), new short[256]));
        store.saveInfo(new ReplayStore.Info(1, "26.3", round, List.of(player), 4));

        var loaded = store.load(round, player, "26.3");
        assertEquals(2, loaded.startFrame().tick());
        assertEquals(actor, loaded.startActor(player));
        assertEquals(List.of(initial, active), loaded.frames());
        var timeline = new ReplayTimeline(loaded.frames(), loaded.info().duration());
        List<ReplayFrame.BlockChange> changes = new ArrayList<>();
        assertEquals(active, timeline.moveTo(loaded.startFrame().tick(), (block, reverse) -> {
            assertFalse(reverse);
            changes.add(block);
        }));
        assertEquals(List.of(change), changes);
        assertTrue(timeline.equipment(player).isEmpty());
        assertThrows(IOException.class, () -> store.load(round, UUID.randomUUID(), "26.3"));
    }

    @Test
    void rejectsTrulyEmptyAndMobOnlyReplaysWithoutMistakingMobsForParticipants() throws Exception {
        UUID round = UUID.randomUUID(), player = UUID.randomUUID(), world = UUID.randomUUID();
        ReplayStore store = new ReplayStore(directory);
        try (var writer = store.writer(round, false)) {
            store.writeFrame(writer, new ReplayFrame(0, List.of(), List.of(), List.of()));
        }
        store.saveInfo(new ReplayStore.Info(1, "26.3", round, List.of(player), 2));
        assertEquals("Replay contains no recorded participants", assertThrows(IOException.class, () -> store.load(round, player, "26.3")).getMessage());

        var mob = new ReplayFrame.Actor(player, world, "ZOMBIE", null, null, null, null,
                0, 64, 0, 0, 0, "STANDING", false, false, List.of());
        try (var writer = store.writer(round, false)) { store.writeFrame(writer, new ReplayFrame(0, List.of(mob), List.of(), List.of())); }
        store.saveInfo(new ReplayStore.Info(1, "26.3", round, List.of(player), 2));
        assertEquals("Replay contains no recorded participants", assertThrows(IOException.class, () -> store.load(round, player, "26.3")).getMessage());

        try (var writer = store.writer(round, false)) { writer.flush(); }
        store.saveInfo(new ReplayStore.Info(1, "26.3", round, List.of(player), 0));
        assertEquals("Replay contains no frames", assertThrows(IOException.class, () -> store.load(round, player, "26.3")).getMessage());
    }

    @Test
    void replaySurvivesRestartAndOnlyItsParticipantsCanReadIt() throws Exception {
        UUID round = UUID.randomUUID(), player = UUID.randomUUID(), world = UUID.randomUUID();
        ReplayStore store = new ReplayStore(directory);
        var actor = new ReplayFrame.Actor(player, world, "PLAYER", "Katze", null, "skin", "signature",
                -1, 64, 0, 0, 0, "STANDING", false, false, List.of(""));
        var frame = new ReplayFrame(0, List.of(actor), List.of(), List.of(new ReplayFrame.Scene(world, "NORMAL", 1000, false, false)));
        var key = new ReplayStore.ChunkKey(world, -1, 0);
        var chunk = new ReplayChunk(-64, -63, List.of("minecraft:stone"), new short[256]);
        try (var writer = store.writer(round, false)) { store.writeFrame(writer, frame); }
        store.writeChunk(round, key, chunk);
        store.saveRecordingState(round, Map.of(key, chunk));
        assertFalse(store.available(round));
        store.saveInfo(new ReplayStore.Info(1, "26.3", round, List.of(player), 2));

        ReplayStore restarted = new ReplayStore(directory);
        assertTrue(restarted.available(round));
        assertEquals(frame, restarted.load(round, player, "26.3").frames().getFirst());
        assertEquals(List.of(), restarted.load(round, player, "26.3").chunks().get(key).changes(world, -1, 0, chunk));
        assertThrows(IOException.class, () -> restarted.load(round, UUID.randomUUID(), "26.3"));
        assertThrows(IOException.class, () -> restarted.load(round, player, "26.1"));

        try (var writer = restarted.writer(round, true)) {
            assertFalse(restarted.available(round));
            restarted.writeFrame(writer, new ReplayFrame(2, List.of(actor), List.of(), frame.scenes()));
        }
        restarted.saveInfo(new ReplayStore.Info(1, "26.3", round, List.of(player), 4));
        assertEquals(List.of(0, 2), restarted.load(round, player, "26.3").frames().stream().map(ReplayFrame::tick).toList());
        assertEquals(1, restarted.recordingState(round).size());
    }

    @Test
    void chunkPaletteRoundTripPreservesNegativeCoordinatesAndBlockProperties() throws Exception {
        short[] before = new short[512], after = new short[512];
        after[0] = 1;
        after[511] = 1;
        List<String> palette = List.of("minecraft:air", "minecraft:oak_stairs[facing=west,half=top,shape=straight,waterlogged=true]");
        ReplayChunk original = new ReplayChunk(-64, -62, palette, before);
        ReplayChunk changed = new ReplayChunk(-64, -62, palette, after);
        Path file = directory.resolve("chunk.gz");
        changed.write(file);
        UUID world = UUID.randomUUID();
        assertEquals(List.of(
                new ReplayFrame.BlockChange(world, -32, -64, -16, palette.getFirst(), palette.get(1)),
                new ReplayFrame.BlockChange(world, -17, -63, -1, palette.getFirst(), palette.get(1))),
                ReplayChunk.read(file).changes(world, -2, -1, original));
    }

    @Test
    void corruptChunkRunsFailWithoutReadingOutsideTheirPalette() throws Exception {
        Path file = directory.resolve("broken.gz");
        try (var out = new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(file)))) {
            out.writeInt(0);
            out.writeInt(1);
            out.writeInt(1);
            out.writeUTF("minecraft:air");
            out.writeShort(0);
            out.writeInt(257);
        }
        assertThrows(IOException.class, () -> ReplayChunk.read(file));
    }

    @Test
    void incrementalBlockChangesRemainConsistentWithTheNextFullSnapshot() {
        UUID world = UUID.randomUUID();
        ReplayChunk chunk = new ReplayChunk(-64, -63, List.of("minecraft:air"), new short[256]);
        assertEquals(new ReplayFrame.BlockChange(world, -1, -64, -1, "minecraft:air", "minecraft:stone"),
                chunk.update(world, -1, -64, -1, "minecraft:stone"));
        assertNull(chunk.update(world, -1, -64, -1, "minecraft:stone"));
        short[] blocks = new short[256];
        blocks[255] = 1;
        ReplayChunk next = new ReplayChunk(-64, -63, List.of("minecraft:air", "minecraft:stone"), blocks);
        assertEquals(List.of(), next.changes(world, -1, -1, chunk));
        assertEquals("minecraft:stone", chunk.update(world, -1, -64, -1, "minecraft:air").before());
    }
}
