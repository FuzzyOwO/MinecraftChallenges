package gg.itzkatze.katze.replay;

import com.google.gson.Gson;
import com.github.luben.zstd.ZstdInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ReplayArchiveTest {
    @TempDir Path directory;

    @Test void pagesPreserveAllActorDataAndSupportBackwardSeekingAndAppend() throws Exception {
        UUID round = UUID.randomUUID(), player = UUID.randomUUID(), world = UUID.randomUUID();
        var state = new ReplayFrame.PlayerState(List.of("item", ""), List.of("backpack"), 2, 17, 20, 19, 5, "world",
                List.of("information"), Map.of("FORCEBATTLE", List.of("scoreboard")));
        var actor = new ReplayFrame.Actor(player, world, "PLAYER", "Katze", "snapshot", "texture", "signature", 1, 64, -1,
                34, 12, "SITTING", true, true, List.of("equipment"), UUID.randomUUID(), true, 45, "dropped", state);
        var frames = new ArrayList<ReplayFrame>();
        for (int tick = 0; tick < 301; tick++) frames.add(new ReplayFrame(tick, List.of(actor),
                tick == 100 ? List.of(new ReplayFrame.BlockChange(world, 1, 64, 1, "air", "stone")) : List.of(),
                List.of(new ReplayFrame.Scene(world, "NORMAL", tick, true, true)), List.of(new ReplayFrame.Mining(player, world, 1, 64, 1, 0.5f)),
                tick >= 100 && tick < 200 ? List.of(new ReplayFrame.Container(world, -1, 64, 0)) : List.of()));
        Path path = directory.resolve("frames.bin");
        try (var writer = new ReplayArchive.Writer(path, false)) { for (var frame : frames.subList(0, 201)) writer.write(frame); }
        try (var writer = new ReplayArchive.Writer(path, true)) { for (var frame : frames.subList(201, 301)) writer.write(frame); }
        var info = new ReplayStore.Info(2, "26.3", round, List.of(player), 301);
        var archive = ReplayArchive.open(path, info);
        assertEquals(frames.size(), archive.size());
        for (int index : List.of(300, 0, 200, 99, 100, 299, 1)) assertEquals(frames.get(index), archive.get(index));
        var applied = new ArrayList<Boolean>();
        var timeline = new ReplayTimeline(archive, 301);
        timeline.moveTo(300, (block, reverse) -> applied.add(reverse));
        assertEquals(state, timeline.player(player));
        timeline.moveTo(10, (block, reverse) -> applied.add(reverse));
        assertEquals(List.of(false, true), applied);
        assertEquals(10, timeline.currentFrame().tick());
        timeline.moveTo(150, (block, reverse) -> {});
        assertEquals(List.of(new ReplayFrame.Container(world, -1, 64, 0)), timeline.currentFrame().containers());
        timeline.moveTo(200, (block, reverse) -> {});
        assertTrue(timeline.currentFrame().containers().isEmpty());
        var seek = ReplaySeek.prepare(timeline, 10);
        var terrain = new ArrayList<String>();
        assertTrue(seek.apply(128, Long.MAX_VALUE, () -> 0, block -> terrain.add(block.after())));
        assertEquals(List.of("air"), terrain);
        assertEquals(200, timeline.currentFrame().tick());
        assertEquals(10, seek.timeline().currentFrame().tick());
        assertEquals(state, seek.timeline().player(player));
        assertTrue(seek.timeline().currentFrame().containers().isEmpty());
    }

    @Test void legacyRecordingConvertsBeforeDeletingItsOriginalAndUsesLessDisk() throws Exception {
        UUID round = UUID.randomUUID(), player = UUID.randomUUID(), world = UUID.randomUUID();
        var store = new ReplayStore(directory);
        Files.createDirectories(store.path(round));
        Path source = store.path(round).resolve("frames.gz");
        var gson = new Gson();
        var random = new Random(42);
        var items = new ArrayList<String>();
        for (int i = 0; i < 36; i++) {
            byte[] bytes = new byte[1024]; random.nextBytes(bytes);
            items.add(Base64.getEncoder().encodeToString(bytes));
        }
        var state = new ReplayFrame.PlayerState(items, null, null, 20, 20, 20, 0, "world", List.of());
        var actor = new ReplayFrame.Actor(player, world, "PLAYER", "Katze", null, null, null, 0, 64, 0,
                0, 0, "STANDING", false, false, null, null, false, 0, null, state);
        try (var writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(source)), StandardCharsets.UTF_8))) {
            for (int tick = 0; tick < 250; tick++) { gson.toJson(new ReplayFrame(tick, List.of(actor), List.of(), List.of()), writer); writer.newLine(); }
        }
        long legacySize = Files.size(source);
        store.saveInfo(new ReplayStore.Info(1, "26.3", round, List.of(player), 250));
        store.writeChunk(round, new ReplayStore.ChunkKey(world, 0, 0), new ReplayChunk(64, 65, List.of("minecraft:air"), new short[256]));
        var loaded = store.load(round, player, "26.3");
        assertEquals(state, loaded.frames().get(249).actors().getFirst().player());
        assertFalse(Files.exists(source));
        assertTrue(Files.size(store.path(round).resolve("frames.bin")) < legacySize / 10);
        assertEquals(250, store.load(round, player, "26.3").frames().size());
    }

    @Test void failedConversionPreservesTheOriginalRecording() throws Exception {
        UUID round = UUID.randomUUID(), player = UUID.randomUUID();
        var store = new ReplayStore(directory);
        Files.createDirectories(store.path(round));
        Path source = store.path(round).resolve("frames.gz");
        try (var out = new GZIPOutputStream(Files.newOutputStream(source))) { out.write("{\"tick\":".getBytes(StandardCharsets.UTF_8)); }
        store.saveInfo(new ReplayStore.Info(1, "26.3", round, List.of(player), 100));
        assertThrows(IOException.class, () -> store.load(round, player, "26.3"));
        assertTrue(Files.isRegularFile(source));
        assertFalse(Files.exists(store.path(round).resolve("frames.bin")));
        assertFalse(Files.exists(store.path(round).resolve("frames.migrating")));
    }

    @Test void truncatedPagesFailBeforePlayback() throws Exception {
        Path path = directory.resolve("frames.bin");
        UUID player = UUID.randomUUID(), world = UUID.randomUUID();
        var actor = new ReplayFrame.Actor(player, world, "PLAYER", "Katze", null, null, null, 0, 64, 0, 0, 0, "STANDING", false, false, null);
        try (var writer = new ReplayArchive.Writer(path, false)) { writer.write(new ReplayFrame(0, List.of(actor), List.of(), List.of())); }
        try (var file = new RandomAccessFile(path.toFile(), "rw")) { file.setLength(file.length() - 1); }
        assertThrows(IOException.class, () -> ReplayArchive.open(path, new ReplayStore.Info(2, "26.3", UUID.randomUUID(), List.of(player), 1)));
    }

    @Test void indexedStartupSkipsLaterPagesButValidatesThemWhenPlayed() throws Exception {
        UUID player = UUID.randomUUID(), world = UUID.randomUUID();
        Path path = directory.resolve("indexed.bin");
        var actor = new ReplayFrame.Actor(player, world, "PLAYER", "Katze", null, null, null, 0, 64, 0, 0, 0, "STANDING", false, false, null);
        try (var writer = new ReplayArchive.Writer(path, false)) {
            for (int tick = 0; tick < 301; tick++) writer.write(new ReplayFrame(tick, List.of(actor), List.of(), List.of()));
        }
        assertTrue(Files.isRegularFile(ReplayArchive.indexPath(path)));
        var modified = Files.getLastModifiedTime(path);
        try (var file = new RandomAccessFile(path.toFile(), "rw")) {
            file.seek(file.length() - 1);
            int value = file.read();
            file.seek(file.length() - 1);
            file.write(value ^ 255);
        }
        Files.setLastModifiedTime(path, modified);
        var info = new ReplayStore.Info(2, "26.3", UUID.randomUUID(), List.of(player), 301);
        var archive = ReplayArchive.open(path, info);
        assertEquals(301, archive.size());
        assertEquals(actor, archive.start().actors().getFirst());
        assertThrows(UncheckedIOException.class, () -> archive.get(300));
        Files.delete(ReplayArchive.indexPath(path));
        assertThrows(IOException.class, () -> ReplayArchive.open(path, info));
    }

    @Test void oldArchivesRebuildTheirIndexAndRetainDimensionsVisitedLater() throws Exception {
        UUID player = UUID.randomUUID(), world = UUID.randomUUID(), nether = UUID.randomUUID();
        Path path = directory.resolve("old.bin");
        var actor = new ReplayFrame.Actor(player, world, "PLAYER", "Katze", null, null, null, 0, 64, 0, 0, 0, "STANDING", false, false, null);
        var normal = new ReplayFrame.Scene(world, "NORMAL", 1000, false, false);
        var late = new ReplayFrame.Scene(nether, "NETHER", 0, false, false);
        try (var writer = new ReplayArchive.Writer(path, false)) {
            for (int tick = 0; tick < 201; tick++) writer.write(new ReplayFrame(tick, List.of(actor), List.of(), List.of(tick < 150 ? normal : late)));
        }
        var info = new ReplayStore.Info(2, "26.3", UUID.randomUUID(), List.of(player), 201);
        Files.delete(ReplayArchive.indexPath(path));
        assertEquals(2, ReplayArchive.open(path, info).scenes().size());
        assertTrue(Files.isRegularFile(ReplayArchive.indexPath(path)));
        Files.writeString(ReplayArchive.indexPath(path), "invalid index");
        assertEquals(List.of(actor), ReplayArchive.open(path, info).get(200).actors());
        assertEquals(2, ReplayArchive.open(path, info).scenes().size());
    }

    @Test void ninetyMinuteTwelvePlayerReplayFitsIn64MiBHeap() throws Exception {
        String classpath = String.join(System.getProperty("path.separator"),
                Path.of(ReplayArchiveStressProbe.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString(),
                Path.of(ReplayArchive.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString(),
                Path.of(ZstdInputStream.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString());
        Path output = directory.resolve("stress.log");
        var process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(), "-Xmx64m", "-cp", classpath,
                ReplayArchiveStressProbe.class.getName(), directory.resolve("long.bin").toString()).redirectErrorStream(true).redirectOutput(output.toFile()).start();
        try {
            assertTrue(process.waitFor(120, TimeUnit.SECONDS), "Long replay probe timed out");
            assertEquals(0, process.exitValue(), () -> {
                try { return Files.readString(output); } catch (Exception e) { return e.toString(); }
            });
            System.out.print(Files.readString(output));
        } finally { process.destroyForcibly(); }
    }
}
