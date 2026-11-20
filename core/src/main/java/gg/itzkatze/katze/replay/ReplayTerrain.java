package gg.itzkatze.katze.replay;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

final class ReplayTerrain {
    private final Path directory;
    private final Path baseline;
    private final boolean resumed;
    private final Set<ReplayStore.ChunkKey> saved = new HashSet<>();
    private final Set<ReplayStore.ChunkKey> dirty = new HashSet<>();
    private final Map<ReplayStore.ChunkKey, ReplayChunk> cache = new LinkedHashMap<>(32, 0.75f, true);

    ReplayTerrain(Path directory, boolean resumed) {
        this(directory, null, resumed);
    }

    ReplayTerrain(Path directory, Path baseline, boolean resumed) {
        this.directory = directory;
        this.baseline = baseline;
        this.resumed = resumed;
    }

    ReplayChunk get(ReplayStore.ChunkKey key) throws IOException {
        ReplayChunk chunk = cache.get(key);
        if (chunk == null && (resumed || saved.contains(key)) && Files.isRegularFile(file(key))) {
            chunk = ReplayChunk.read(file(key));
            put(key, chunk);
        }
        if (chunk == null && baseline != null && Files.isRegularFile(baseline.resolve(key.fileName()))) {
            chunk = ReplayChunk.read(baseline.resolve(key.fileName()));
            put(key, chunk);
        }
        return chunk;
    }

    ReplayChunk replace(ReplayStore.ChunkKey key, ReplayChunk chunk) throws IOException {
        ReplayChunk previous = get(key);
        if (previous != null && previous.sameContents(chunk)) return previous;
        if (previous != null || baseline == null) dirty.add(key);
        put(key, chunk);
        return previous;
    }

    void changed(ReplayStore.ChunkKey key) { dirty.add(key); }

    private void put(ReplayStore.ChunkKey key, ReplayChunk chunk) throws IOException {
        cache.put(key, chunk);
        if (cache.size() > 32) {
            var oldest = cache.entrySet().iterator().next();
            if (dirty.contains(oldest.getKey())) save(oldest.getKey(), oldest.getValue());
            cache.remove(oldest.getKey());
        }
    }

    void flush() throws IOException {
        for (var entry : cache.entrySet()) if (dirty.contains(entry.getKey())) save(entry.getKey(), entry.getValue());
    }

    void clear() { cache.clear(); saved.clear(); dirty.clear(); }
    private Path file(ReplayStore.ChunkKey key) { return directory.resolve(key.fileName()); }
    private void save(ReplayStore.ChunkKey key, ReplayChunk chunk) throws IOException {
        chunk.write(file(key));
        saved.add(key);
        dirty.remove(key);
    }
}
