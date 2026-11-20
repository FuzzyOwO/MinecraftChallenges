package gg.itzkatze.katze.replay;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ReplayCompactionTest {
    @TempDir Path directory;

    @Test void oldBinaryRecordingsAreVerifiedAndCompactedWithoutChangingFramesOrResumeTerrain() throws Exception {
        UUID round = UUID.randomUUID(), player = UUID.randomUUID(), world = UUID.randomUUID();
        var store = new ReplayStore(directory);
        var frames = frames(player, world, 0, 240);
        Path folder = store.path(round);
        Files.createDirectories(folder);
        legacy(folder.resolve("frames.bin"), frames);
        var key = new ReplayStore.ChunkKey(world, 0, 0);
        var chunk = new ReplayChunk(0, 1, List.of("minecraft:air"), new short[256]);
        store.writeChunk(round, key, chunk);
        store.saveRecordingState(round, Map.of(key, chunk));
        store.saveInfo(new ReplayStore.Info(3, "26.3", round, List.of(player), 240));
        long original = Files.size(folder.resolve("frames.bin"));
        var loaded = store.load(round, player, "26.3");
        assertEquals(4, loaded.info().format());
        assertEquals(frames, loaded.frames());
        assertTrue(Files.size(folder.resolve("frames.bin")) + Files.size(folder.resolve("strings.bin")) < original);
        System.out.println("Verified old recording compaction: " + original + " -> "
                + (Files.size(folder.resolve("frames.bin")) + Files.size(folder.resolve("strings.bin"))) + " bytes");
        assertFalse(Files.exists(folder.resolve("state").resolve(key.fileName())));
        assertEquals(1, store.recordingState(round).size());
        assertEquals(frames, new ReplayStore(directory).load(round, player, "26.3").frames());
        assertFalse(Files.exists(folder.resolve("frames.compacting")));
    }

    @Test void largePayloadReferencesSurvivePageEvictionAndAppend() throws Exception {
        UUID player = UUID.randomUUID(), world = UUID.randomUUID();
        Path path = directory.resolve("frames.bin");
        var frames = frames(player, world, 0, 240);
        try (var writer = new ReplayArchive.Writer(path, false)) { for (var frame : frames.subList(0, 150)) writer.write(frame); }
        try (var writer = new ReplayArchive.Writer(path, true)) { for (var frame : frames.subList(150, 240)) writer.write(frame); }
        var archive = ReplayArchive.open(path, new ReplayStore.Info(4, "26.3", UUID.randomUUID(), List.of(player), 240));
        for (int index : new int[]{239, 0, 149, 150, 20, 220}) assertEquals(frames.get(index), archive.get(index));
        var strings = new ReplayStrings(directory.resolve("strings.bin"), true);
        int id = strings.id("item-data".repeat(1000));
        assertEquals(id, strings.id("item-data".repeat(1000)));
        assertEquals("item-data".repeat(1000), new ReplayStrings(directory.resolve("strings.bin"), false).get(id));
    }

    private List<ReplayFrame> frames(UUID player, UUID world, int first, int end) {
        List<ReplayFrame> frames = new ArrayList<>();
        for (int tick = first; tick < end; tick++) {
            var state = new ReplayFrame.PlayerState(List.of("item-data".repeat(1000)), List.of(), 1, 20, 20, 20, 2, "world", List.of());
            var actor = new ReplayFrame.Actor(player, world, "PLAYER", "Katze", null, tick == 0 ? "skin".repeat(100) : null, null,
                    tick * 0.1, 90, 0, 0, 0, "STANDING", false, false, null, null, false, 0, null, tick % 10 == 0 ? state : null);
            frames.add(new ReplayFrame(tick, List.of(actor), List.of(), List.of(new ReplayFrame.Scene(world, "NORMAL", 6000, false, false))));
        }
        return frames;
    }

    private void legacy(Path path, List<ReplayFrame> frames) throws Exception {
        try (var file = new DataOutputStream(Files.newOutputStream(path))) {
            file.writeInt(0x4b475232);
            for (int start = 0; start < frames.size(); start += 100) {
                var raw = new ByteArrayOutputStream();
                var codec = new ReplayFrameCodec();
                try (var out = new DataOutputStream(raw)) {
                    for (var frame : frames.subList(start, Math.min(start + 100, frames.size()))) codec.write(out, frame);
                }
                var packed = new ByteArrayOutputStream();
                try (var gzip = new GZIPOutputStream(packed)) { gzip.write(raw.toByteArray()); }
                file.writeInt(packed.size()); file.writeInt(raw.size()); file.writeInt(Math.min(100, frames.size() - start));
                file.write(packed.toByteArray());
            }
        }
    }
}
