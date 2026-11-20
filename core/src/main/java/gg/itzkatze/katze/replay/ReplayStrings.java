package gg.itzkatze.katze.replay;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ReplayStrings {
    private static final int MAGIC = 0x4b475331;
    private static final int MAX_BYTES = 16 * 1024 * 1024;
    private final Path path;
    private final List<Long> offsets = new ArrayList<>();
    private final Map<String, List<Integer>> hashes = new HashMap<>();
    private final Map<Integer, String> cache = new LinkedHashMap<>(128, 0.75f, true);
    private int cachedBytes;

    ReplayStrings(Path path, boolean writing) throws IOException {
        this.path = path;
        if (!Files.exists(path) && writing) {
            try (var file = new RandomAccessFile(path.toFile(), "rw")) { file.writeInt(MAGIC); }
        }
        try (var file = new RandomAccessFile(path.toFile(), "r")) {
            if (file.readInt() != MAGIC) throw new IOException("Invalid replay string pool");
            while (file.getFilePointer() < file.length()) {
                offsets.add(file.getFilePointer());
                int raw = file.readInt(), packed = file.readInt();
                if (raw < 0 || raw > MAX_BYTES || packed < 1 || packed > MAX_BYTES + 65536 || packed > file.length() - file.getFilePointer()) throw new IOException("Invalid replay string entry");
                file.seek(file.getFilePointer() + packed);
            }
        }
        if (writing) for (int id = 0; id < offsets.size(); id++) hashes.computeIfAbsent(hash(get(id)), ignored -> new ArrayList<>()).add(id);
    }

    int id(String value) throws IOException {
        String hash = hash(value);
        List<Integer> candidates = hashes.computeIfAbsent(hash, ignored -> new ArrayList<>());
        for (int candidate : candidates) if (get(candidate).equals(value)) return candidate;
        byte[] raw = value.getBytes(StandardCharsets.UTF_8);
        if (raw.length > MAX_BYTES) throw new IOException("Replay string is too large");
        var packed = new ByteArrayOutputStream();
        try (var out = ReplayCompression.write(packed)) { out.write(raw); }
        int id = offsets.size();
        try (var file = new RandomAccessFile(path.toFile(), "rw")) {
            long offset = file.length();
            file.seek(offset);
            file.writeInt(raw.length); file.writeInt(packed.size()); file.write(packed.toByteArray());
            offsets.add(offset);
        }
        candidates.add(id);
        cache(id, value);
        return id;
    }

    String get(int id) throws IOException {
        if (id < 0 || id >= offsets.size()) throw new IOException("Invalid replay string reference");
        String cached = cache.get(id);
        if (cached != null) return cached;
        try (var file = new RandomAccessFile(path.toFile(), "r")) {
            file.seek(offsets.get(id));
            int raw = file.readInt(), size = file.readInt();
            byte[] packed = new byte[size]; file.readFully(packed);
            byte[] decoded;
            try (var input = ReplayCompression.read(new ByteArrayInputStream(packed))) { decoded = input.readNBytes(raw + 1); }
            if (decoded.length != raw) throw new IOException("Invalid replay string length");
            String value = new String(decoded, StandardCharsets.UTF_8);
            cache(id, value);
            return value;
        }
    }

    private void cache(int id, String value) {
        String previous = cache.put(id, value);
        cachedBytes += value.length() * 2 - (previous == null ? 0 : previous.length() * 2);
        while (cachedBytes > 8 * 1024 * 1024 && cache.size() > 1) {
            var oldest = cache.entrySet().iterator().next();
            cachedBytes -= oldest.getValue().length() * 2;
            cache.remove(oldest.getKey());
        }
    }

    private static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
