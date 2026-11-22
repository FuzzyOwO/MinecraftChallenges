package gg.itzkatze.katze.replay;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

record ReplayCheckpoint(int index, ReplayFrame seed, ReplayFrame frame) {
    private static final int MAGIC = 0x4b474331;

    void write(Path path) throws IOException {
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            try (var out = new DataOutputStream(ReplayCompression.write(Files.newOutputStream(temporary)))) {
                out.writeInt(MAGIC);
                out.writeInt(index);
                Path pool = path.resolveSibling("strings.bin");
                var codec = new ReplayPackedCodec(Files.isRegularFile(pool) ? new ReplayStrings(pool, true) : null);
                codec.write(out, seed);
                codec.write(out, frame);
            }
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temporary); }
    }

    static ReplayCheckpoint read(Path path) throws IOException {
        try (var in = new DataInputStream(ReplayCompression.read(Files.newInputStream(path)))) {
            if (in.readInt() != MAGIC) throw new IOException("Invalid replay checkpoint");
            int index = in.readInt();
            Path pool = path.resolveSibling("strings.bin");
            var codec = new ReplayFrameCodec(Files.isRegularFile(pool) ? new ReplayStrings(pool, false) : null);
            ReplayFrame seed = codec.read(in), frame = codec.read(in);
            if (index < 0 || seed.tick() != frame.tick() || in.read() != -1) throw new IOException("Invalid replay checkpoint state");
            return new ReplayCheckpoint(index, seed, frame);
        }
    }

    static final class Builder {
        private final Map<UUID, ReplayFrame.Actor> definitions = new HashMap<>();
        private final Map<UUID, List<String>> equipment = new HashMap<>();
        private final Map<UUID, ReplayFrame.PlayerState> players = new HashMap<>();
        private final Map<UUID, String> items = new HashMap<>();
        private final ReplayTerrainState terrain = new ReplayTerrainState();
        private int index = -1;
        private ReplayFrame current;

        void add(ReplayFrame frame) {
            index++;
            current = frame;
            for (var actor : frame.actors()) {
                definitions.putIfAbsent(actor.id(), actor);
                if (actor.equipment() != null) equipment.put(actor.id(), actor.equipment());
                if (actor.player() != null) players.put(actor.id(), actor.player());
                if (actor.item() != null) items.put(actor.id(), actor.item());
            }
            frame.blocks().forEach(block -> terrain.apply(block, false));
        }

        ReplayCheckpoint build() {
            List<ReplayFrame.Actor> actors = definitions.values().stream().map(actor -> new ReplayFrame.Actor(actor.id(), actor.world(), actor.type(), actor.name(),
                    actor.snapshot(), actor.texture(), actor.signature(), actor.x(), actor.y(), actor.z(), actor.yaw(), actor.pitch(), actor.pose(), actor.burning(),
                    actor.swinging(), equipment.get(actor.id()), actor.vehicle(), actor.hurt(), actor.hurtDirection(), items.get(actor.id()), players.get(actor.id()), actor.health(), actor.maxHealth())).toList();
            return new ReplayCheckpoint(index, new ReplayFrame(current.tick(), actors, terrain.snapshot(), current.scenes()), current);
        }
    }
}
