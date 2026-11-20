package gg.itzkatze.katze.replay;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class ReplayChunkFiles extends AbstractMap<ReplayStore.ChunkKey, ReplayChunk> {
    private final Map<ReplayStore.ChunkKey, Path> files = new HashMap<>();
    private final Map<ReplayStore.ChunkKey, ReplayChunk> cache = new LinkedHashMap<>(32, 0.75f, true);

    ReplayChunkFiles(Path directory) throws IOException {
        try (var paths = Files.list(directory)) {
            for (Path file : paths.toList()) {
                String[] parts = file.getFileName().toString().replace(".gz", "").split("_");
                if (parts.length != 3) throw new IOException("Invalid chunk name");
                try { files.put(new ReplayStore.ChunkKey(UUID.fromString(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])), file); }
                catch (IllegalArgumentException e) { throw new IOException("Invalid chunk name", e); }
            }
        }
    }

    @Override public synchronized ReplayChunk get(Object key) {
        Path file = files.get(key);
        if (file == null) return null;
        var chunk = cache.get(key);
        if (chunk == null) {
            try { chunk = ReplayChunk.read(file); }
            catch (IOException e) { throw new UncheckedIOException("Failed to read replay terrain", e); }
            cache.put((ReplayStore.ChunkKey) key, chunk);
            if (cache.size() > 32) cache.remove(cache.keySet().iterator().next());
        }
        return chunk;
    }

    @Override public int size() { return files.size(); }
    @Override public boolean containsKey(Object key) { return files.containsKey(key); }
    @Override public Set<Entry<ReplayStore.ChunkKey, ReplayChunk>> entrySet() {
        return new AbstractSet<>() {
            @Override public int size() { return files.size(); }
            @Override public Iterator<Entry<ReplayStore.ChunkKey, ReplayChunk>> iterator() {
                var keys = files.keySet().iterator();
                return new Iterator<>() {
                    @Override public boolean hasNext() { return keys.hasNext(); }
                    @Override public Entry<ReplayStore.ChunkKey, ReplayChunk> next() {
                        var key = keys.next();
                        return Map.entry(key, get(key));
                    }
                };
            }
        };
    }
}
