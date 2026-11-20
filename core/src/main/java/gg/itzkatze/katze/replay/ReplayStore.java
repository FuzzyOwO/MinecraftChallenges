package gg.itzkatze.katze.replay;

import com.google.gson.Gson;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import gg.itzkatze.katze.util.JsonFiles;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardOpenOption;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

public final class ReplayStore {
    private static final Gson GSON = new Gson();
    private final Path directory;

    public record Info(int format, String minecraft, UUID round, List<UUID> participants, int duration, Integer combatTick) {
        public Info(int format, String minecraft, UUID round, List<UUID> participants, int duration) {
            this(format, minecraft, round, participants, duration, null);
        }
    }
    record ChunkKey(UUID world, int x, int z) {
        String fileName() { return world + "_" + x + "_" + z + ".gz"; }
    }
    record Loaded(Info info, Map<ChunkKey, ReplayChunk> chunks, List<ReplayFrame> frames, ReplayFrame startFrame, ReplayCheckpoint combat) {
        Loaded(Info info, Map<ChunkKey, ReplayChunk> chunks, List<ReplayFrame> frames, ReplayFrame startFrame) {
            this(info, chunks, frames, startFrame, null);
        }
        List<ReplayFrame.Scene> scenes() {
            return frames instanceof ReplayArchive archive ? archive.scenes() : frames.stream().flatMap(frame -> frame.scenes().stream()).distinct().toList();
        }
        ReplayFrame.Actor startActor(UUID viewer) {
            return startFrame.actors().stream().filter(actor -> actor.participant(info.participants()) && actor.id().equals(viewer))
                    .findFirst().orElseGet(() -> startFrame.actors().stream().filter(actor -> actor.participant(info.participants())).findFirst().orElseThrow());
        }
    }

    public ReplayStore(Path directory) { this.directory = directory; }

    Path path(UUID round) { return directory.resolve(round.toString()); }

    public boolean available(UUID round) { return Files.isRegularFile(path(round).resolve("info.json")); }

    Info info(UUID round) throws IOException { return JsonFiles.read(path(round).resolve("info.json"), Info.class); }

    ReplayArchive.Writer writer(UUID round, boolean append) throws IOException {
        Files.createDirectories(path(round));
        if (append) migrate(round, info(round));
        Files.deleteIfExists(path(round).resolve("info.json"));
        if (!append) Files.deleteIfExists(path(round).resolve("combat.bin"));
        return new ReplayArchive.Writer(path(round).resolve("frames.bin"), append);
    }

    void writeFrame(ReplayArchive.Writer writer, ReplayFrame frame) throws IOException { writer.write(frame); }

    void saveInfo(Info info) throws IOException { JsonFiles.write(path(info.round()).resolve("info.json"), info); }

    void writeChunk(UUID round, ChunkKey key, ReplayChunk chunk) throws IOException {
        chunk.write(path(round).resolve("chunks").resolve(key.fileName()));
    }

    void saveRecordingState(UUID round, Map<ChunkKey, ReplayChunk> chunks) throws IOException {
        for (var entry : chunks.entrySet()) entry.getValue().write(path(round).resolve("state").resolve(entry.getKey().fileName()));
    }

    Map<ChunkKey, ReplayChunk> recordingState(UUID round) throws IOException {
        Map<ChunkKey, ReplayChunk> chunks = new HashMap<>(readChunks(path(round).resolve("chunks")));
        chunks.putAll(readChunks(path(round).resolve("state")));
        return chunks;
    }

    synchronized Loaded load(UUID round, UUID viewer, String minecraft) throws IOException {
        Info info = info(round);
        if (info == null || info.format() < 1 || info.format() > 4 || !minecraft.equals(info.minecraft()) || !round.equals(info.round()) || !info.participants().contains(viewer)) {
            throw new IOException("Replay unavailable for this player or Minecraft version");
        }
        migrate(round, info);
        if (info.format() < 4) info = compact(round, info);
        ReplayArchive frames = ReplayArchive.open(path(round).resolve("frames.bin"), info);
        return new Loaded(info, new ReplayChunkFiles(path(round).resolve("chunks")), frames, frames.start());
    }

    private Info compact(UUID round, Info info) throws IOException {
        Path original = path(round).resolve("frames.bin"), temporary = path(round).resolve("frames.compacting");
        Path strings = ReplayArchive.stringPath(original);
        long previousStrings = Files.isRegularFile(strings) ? Files.size(strings) : 0;
        boolean replaced = false;
        try {
            ReplayArchive source = ReplayArchive.open(original, info);
            try (var writer = new ReplayArchive.Writer(temporary, false)) {
                for (ReplayFrame frame : source) {
                    if (Thread.currentThread().isInterrupted()) throw new IOException("Replay compaction interrupted");
                    writer.write(frame);
                }
            }
            ReplayArchive candidate = ReplayArchive.verify(temporary, info);
            if (candidate.size() != source.size()) throw new IOException("Replay compaction lost frames");
            for (int index = 0; index < source.size(); index++) {
                if (Thread.currentThread().isInterrupted()) throw new IOException("Replay verification interrupted");
                if (!source.get(index).equals(candidate.get(index))) throw new IOException("Replay compaction changed frame " + index);
            }
            if (Files.size(temporary) + Files.size(strings) - previousStrings < Files.size(original)) {
                try { Files.move(temporary, original, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
                catch (AtomicMoveNotSupportedException e) { Files.move(temporary, original, StandardCopyOption.REPLACE_EXISTING); }
                replaced = true;
                Files.move(ReplayArchive.indexPath(temporary), ReplayArchive.indexPath(original), StandardCopyOption.REPLACE_EXISTING);
            }
            Info packed = new Info(4, info.minecraft(), info.round(), info.participants(), info.duration(), info.combatTick());
            compactTerrain(round);
            saveInfo(packed);
            return packed;
        } finally {
            Files.deleteIfExists(temporary);
            Files.deleteIfExists(ReplayArchive.indexPath(temporary));
            if (!replaced && Files.isRegularFile(strings)) {
                if (previousStrings == 0) Files.delete(strings);
                else try (var channel = FileChannel.open(strings, StandardOpenOption.WRITE)) { channel.truncate(previousStrings); }
            }
        }
    }

    private void compactTerrain(UUID round) throws IOException {
        Path baseline = path(round).resolve("chunks"), state = path(round).resolve("state");
        for (Path directory : List.of(baseline, state)) {
            if (!Files.isDirectory(directory)) continue;
            try (var files = Files.list(directory)) {
                for (Path file : files.filter(path -> path.getFileName().toString().endsWith(".gz")).toList()) {
                    if (Thread.currentThread().isInterrupted()) throw new IOException("Replay terrain compaction interrupted");
                    ReplayChunk chunk = ReplayChunk.read(file);
                    Path initial = baseline.resolve(file.getFileName());
                    if (directory.equals(state) && Files.isRegularFile(initial) && chunk.sameContents(ReplayChunk.read(initial))) {
                        Files.delete(file);
                        continue;
                    }
                    Path temporary = file.resolveSibling(file.getFileName() + ".compacting");
                    try {
                        chunk.write(temporary);
                        if (!chunk.sameContents(ReplayChunk.read(temporary))) throw new IOException("Replay terrain compaction changed " + file);
                        if (Files.size(temporary) < Files.size(file)) {
                            try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
                            catch (AtomicMoveNotSupportedException e) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
                        }
                    } finally { Files.deleteIfExists(temporary); }
                }
            }
        }
    }

    synchronized Loaded withCombatCheckpoint(Loaded replay) throws IOException {
        Path file = path(replay.info().round()).resolve("combat.bin");
        ReplayCheckpoint checkpoint = null;
        if (Files.isRegularFile(file)) {
            try {
                ReplayCheckpoint saved = ReplayCheckpoint.read(file);
                if (saved.index() < replay.frames().size() && replay.frames().get(saved.index()).tick() == saved.frame().tick()) checkpoint = saved;
            } catch (IOException | RuntimeException ignored) { }
        }
        if (checkpoint == null) {
            int combat = ReplayCombat.find(replay);
            if (combat < 0) return replay;
            var builder = new ReplayCheckpoint.Builder();
            for (ReplayFrame frame : replay.frames()) {
                builder.add(frame);
                if (frame.tick() >= combat) break;
            }
            checkpoint = builder.build();
            checkpoint.write(file);
        }
        return new Loaded(replay.info(), replay.chunks(), replay.frames(), replay.startFrame(), checkpoint);
    }

    private void migrate(UUID round, Info info) throws IOException {
        Path target = path(round).resolve("frames.bin"), source = path(round).resolve("frames.gz");
        if (Files.isRegularFile(target)) return;
        if (!Files.isRegularFile(source) || info == null) throw new IOException("Missing replay frames");
        Path temporary = path(round).resolve("frames.migrating");
        try {
            try (var reader = new JsonReader(new BufferedReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(source)), StandardCharsets.UTF_8)));
                 var writer = new ReplayArchive.Writer(temporary, false)) {
                reader.setStrictness(Strictness.LENIENT);
                while (reader.peek() != JsonToken.END_DOCUMENT) writer.write(GSON.fromJson(reader, ReplayFrame.class));
            } catch (RuntimeException e) { throw new IOException("Invalid legacy replay", e); }
            ReplayArchive.verify(temporary, info);
            try { Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temporary, target); }
            Files.move(ReplayArchive.indexPath(temporary), ReplayArchive.indexPath(target), StandardCopyOption.REPLACE_EXISTING);
            Files.delete(source);
        } finally {
            Files.deleteIfExists(temporary);
            Files.deleteIfExists(ReplayArchive.indexPath(temporary));
        }
    }

    private Map<ChunkKey, ReplayChunk> readChunks(Path directory) throws IOException {
        Map<ChunkKey, ReplayChunk> chunks = new HashMap<>();
        if (!Files.isDirectory(directory)) return chunks;
        try (var paths = Files.list(directory)) {
            for (Path file : paths.toList()) {
                String[] parts = file.getFileName().toString().replace(".gz", "").split("_");
                if (parts.length != 3) throw new IOException("Invalid chunk name");
                chunks.put(new ChunkKey(UUID.fromString(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])), ReplayChunk.read(file));
            }
        }
        return chunks;
    }
}
