package gg.itzkatze.katze.replay;

import java.io.ByteArrayInputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

final class ReplayArchive extends AbstractList<ReplayFrame> {
    private static final int MAGIC = 0x4b475232;
    private static final int INDEX_MAGIC = 0x4b474931;
    private static final int PAGE_FRAMES = 100;
    private static final int PAGE_BYTES = 1024 * 1024;
    private static final int MAX_PAGE_BYTES = 32 * 1024 * 1024;
    private final Path path;
    private final ReplayStrings strings;
    private final List<Page> pages = new ArrayList<>();
    private final Map<Integer, List<ReplayFrame>> cache = new LinkedHashMap<>(4, 0.75f, true);
    private final Map<UUID, ReplayFrame.Scene> scenes = new HashMap<>();
    private final Map<UUID, Integer> firstPlayers = new HashMap<>();
    private ReplayFrame start;
    private int size;

    private record Page(long offset, int compressed, int raw, int first, int count, int firstTick, int lastTick) {}

    private ReplayArchive(Path path) throws IOException {
        this.path = path;
        strings = Files.isRegularFile(stringPath(path)) ? new ReplayStrings(stringPath(path), false) : null;
    }

    static Path stringPath(Path path) { return path.resolveSibling("strings.bin"); }

    static ReplayArchive open(Path path, ReplayStore.Info info) throws IOException {
        ReplayArchive archive = metadata(path);
        archive.validate(info);
        return archive;
    }

    static ReplayArchive verify(Path path, ReplayStore.Info info) throws IOException {
        ReplayArchive archive = scan(path);
        archive.validate(info);
        archive.writeIndex();
        return archive;
    }

    private static ReplayArchive metadata(Path path) throws IOException {
        ReplayArchive archive = readIndex(path);
        if (archive != null) return archive;
        archive = scan(path);
        archive.writeIndex();
        return archive;
    }

    private static ReplayArchive scan(Path path) throws IOException {
        ReplayArchive archive = new ReplayArchive(path);
        int previous = -1;
        try (var file = new RandomAccessFile(path.toFile(), "r")) {
            if (file.readInt() != MAGIC) throw new IOException("Invalid replay archive");
            while (file.getFilePointer() < file.length()) {
                int compressed = file.readInt(), raw = file.readInt(), count = file.readInt();
                if (compressed <= 0 || compressed > MAX_PAGE_BYTES || raw <= 0 || raw > MAX_PAGE_BYTES
                        || count <= 0 || count > PAGE_FRAMES || compressed > file.length() - file.getFilePointer()) {
                    throw new IOException("Invalid replay page");
                }
                Page page = new Page(file.getFilePointer(), compressed, raw, archive.size, count, 0, 0);
                List<ReplayFrame> frames = archive.read(page);
                for (int i = 0; i < frames.size(); i++) {
                    ReplayFrame frame = frames.get(i);
                    if (frame.tick() <= previous || (previous == -1 && frame.tick() != 0)) {
                        throw new IOException("Unordered replay timeline");
                    }
                    previous = frame.tick();
                    archive.record(frame, archive.size + i);
                }
                archive.pages.add(new Page(page.offset(), compressed, raw, archive.size, count, frames.getFirst().tick(), frames.getLast().tick()));
                archive.size = Math.addExact(archive.size, count);
                file.seek(page.offset() + compressed);
            }
        }
        return archive;
    }

    private void record(ReplayFrame frame, int index) {
        for (var actor : frame.actors()) if ("PLAYER".equals(actor.type())) firstPlayers.putIfAbsent(actor.id(), index);
        for (var scene : frame.scenes()) scenes.putIfAbsent(scene.world(), scene);
    }

    private void validate(ReplayStore.Info info) throws IOException {
        if (size == 0) throw new IOException("Replay contains no frames");
        if (info.duration() < 0 || pages.getLast().lastTick() > info.duration()) throw new IOException("Unordered replay timeline");
        int first = info.participants().stream().map(firstPlayers::get).filter(Objects::nonNull).min(Integer::compareTo)
                .orElseThrow(() -> new IOException("Replay contains no recorded participants"));
        try { start = get(first); }
        catch (UncheckedIOException e) { throw e.getCause(); }
        if (!start.hasParticipant(info.participants())) throw new IOException("Invalid replay participant index");
    }

    static Path indexPath(Path path) { return path.resolveSibling(path.getFileName() + ".idx"); }

    private static ReplayArchive readIndex(Path path) throws IOException {
        Path index = indexPath(path);
        if (!Files.isRegularFile(index)) return null;
        try (var in = new DataInputStream(new BufferedInputStream(Files.newInputStream(index)));
             var file = new RandomAccessFile(path.toFile(), "r")) {
            if (file.readInt() != MAGIC) throw new IOException("Invalid replay archive");
            long length = file.length();
            if (in.readInt() != INDEX_MAGIC || in.readLong() != length || in.readLong() != Files.getLastModifiedTime(path).toMillis()) return null;
            ReplayArchive archive = new ReplayArchive(path);
            int count = in.readInt();
            if (count < 0 || count > 1_000_000 || count > length / 12) return null;
            long offset = 4;
            int previous = -1;
            for (int i = 0; i < count; i++) {
                Page page = new Page(in.readLong(), in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt());
                if (page.offset() != offset + 12 || page.first() != archive.size || page.count() <= 0 || page.count() > PAGE_FRAMES
                        || page.compressed() <= 0 || page.compressed() > MAX_PAGE_BYTES || page.raw() <= 0 || page.raw() > MAX_PAGE_BYTES
                        || page.firstTick() <= previous || page.lastTick() < page.firstTick() || (i == 0 && page.firstTick() != 0)) return null;
                archive.pages.add(page);
                archive.size = Math.addExact(archive.size, page.count());
                previous = page.lastTick();
                offset = page.offset() + page.compressed();
                if (offset > length) return null;
            }
            if (offset != length) return null;
            var metadata = new ReplayFrameCodec().read(in);
            for (var scene : metadata.scenes()) archive.scenes.put(scene.world(), scene);
            int players = in.readInt();
            if (players < 0 || players > 1_000_000) return null;
            for (int i = 0; i < players; i++) {
                UUID id = new UUID(in.readLong(), in.readLong());
                int frame = in.readInt();
                if (frame < 0 || frame >= archive.size) return null;
                archive.firstPlayers.put(id, frame);
            }
            return in.read() == -1 ? archive : null;
        } catch (IOException | RuntimeException e) { return null; }
    }

    private void writeIndex() throws IOException {
        Path target = indexPath(path), temporary = target.resolveSibling(target.getFileName() + ".tmp");
        try {
            try (var out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(temporary)))) {
                out.writeInt(INDEX_MAGIC); out.writeLong(Files.size(path)); out.writeLong(Files.getLastModifiedTime(path).toMillis());
                out.writeInt(pages.size());
                for (var page : pages) {
                    out.writeLong(page.offset()); out.writeInt(page.compressed()); out.writeInt(page.raw());
                    out.writeInt(page.first()); out.writeInt(page.count()); out.writeInt(page.firstTick()); out.writeInt(page.lastTick());
                }
                new ReplayFrameCodec().write(out, new ReplayFrame(0, List.of(), List.of(), scenes()));
                out.writeInt(firstPlayers.size());
                for (var entry : firstPlayers.entrySet()) {
                    out.writeLong(entry.getKey().getMostSignificantBits()); out.writeLong(entry.getKey().getLeastSignificantBits()); out.writeInt(entry.getValue());
                }
            }
            try { Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }

    ReplayFrame start() { return start; }
    List<ReplayFrame.Scene> scenes() { return List.copyOf(scenes.values()); }
    @Override public int size() { return size; }

    @Override public synchronized ReplayFrame get(int index) {
        Objects.checkIndex(index, size);
        int low = 0, high = pages.size() - 1;
        while (low < high) {
            int middle = (low + high + 1) >>> 1;
            if (pages.get(middle).first() <= index) low = middle;
            else high = middle - 1;
        }
        List<ReplayFrame> frames = cache.get(low);
        if (frames == null) {
            try { frames = read(pages.get(low)); }
            catch (IOException e) { throw new UncheckedIOException("Failed to read replay page", e); }
            cache.put(low, frames);
            if (cache.size() > 2) cache.remove(cache.keySet().iterator().next());
        }
        return frames.get(index - pages.get(low).first());
    }

    private List<ReplayFrame> read(Page page) throws IOException {
        byte[] compressed = new byte[page.compressed()];
        try (var file = new RandomAccessFile(path.toFile(), "r")) {
            file.seek(page.offset());
            file.readFully(compressed);
        }
        byte[] raw;
        try (var gzip = ReplayCompression.read(new ByteArrayInputStream(compressed))) {
            raw = gzip.readNBytes(page.raw() + 1);
        }
        if (raw.length != page.raw()) throw new IOException("Invalid replay page length");
        var codec = new ReplayFrameCodec(strings);
        var frames = new ArrayList<ReplayFrame>();
        try (var in = new DataInputStream(new ByteArrayInputStream(raw))) {
            for (int i = 0; i < page.count(); i++) frames.add(codec.read(in));
            if (in.read() != -1) throw new IOException("Unexpected replay page data");
        }
        return List.copyOf(frames);
    }

    static final class Writer implements Closeable {
        private final DataOutputStream output;
        private final ReplayArchive archive;
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private DataOutputStream page;
        private ReplayPackedCodec codec;
        private int count;
        private int firstTick;
        private int lastTick = -1;
        private long offset;
        private boolean closed;
        private final ReplayStrings strings;

        Writer(Path path, boolean append) throws IOException {
            boolean exists = append && Files.exists(path) && Files.size(path) > 0;
            if (!append && path.getFileName().toString().equals("frames.bin")) Files.deleteIfExists(stringPath(path));
            strings = new ReplayStrings(stringPath(path), true);
            archive = exists ? metadata(path) : new ReplayArchive(path);
            offset = exists ? Files.size(path) : 4;
            if (!archive.pages.isEmpty()) lastTick = archive.pages.getLast().lastTick();
            Files.deleteIfExists(indexPath(path));
            output = new DataOutputStream(Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
                    append ? StandardOpenOption.APPEND : StandardOpenOption.TRUNCATE_EXISTING));
            if (!exists) output.writeInt(MAGIC);
            reset();
        }

        void write(ReplayFrame frame) throws IOException {
            if (closed) throw new IOException("Replay writer is closed");
            if (frame.tick() <= lastTick || (lastTick == -1 && frame.tick() != 0)) throw new IOException("Unordered replay timeline");
            codec.write(page, frame);
            archive.record(frame, archive.size + count);
            if (count == 0) firstTick = frame.tick();
            lastTick = frame.tick();
            count++;
            if (page.size() > MAX_PAGE_BYTES) throw new IOException("Replay frame exceeds page limit");
            if (count >= PAGE_FRAMES || page.size() + codec.externalBytes() >= PAGE_BYTES) flush();
        }

        void flush() throws IOException {
            if (closed || count == 0) return;
            int raw = page.size();
            page.close();
            output.writeInt(bytes.size()); output.writeInt(raw); output.writeInt(count);
            bytes.writeTo(output);
            output.flush();
            archive.pages.add(new Page(offset + 12, bytes.size(), raw, archive.size, count, firstTick, lastTick));
            archive.size = Math.addExact(archive.size, count);
            offset += 12L + bytes.size();
            reset();
        }

        private void reset() throws IOException {
            bytes.reset();
            page = new DataOutputStream(ReplayCompression.write(bytes));
            codec = new ReplayPackedCodec(strings);
            count = 0;
        }

        @Override public void close() throws IOException {
            if (closed) return;
            try { flush(); }
            finally {
                closed = true;
                try { page.close(); } finally { output.close(); }
            }
            archive.writeIndex();
        }
    }
}
