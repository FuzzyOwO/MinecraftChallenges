package gg.itzkatze.katze.replay;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class ReplayPackedCodec {
    static final int MAGIC = 0x4b475034;
    private final ReplayFrameCodec tail;
    private final ReplayFrameCodec payload;
    private final Map<UUID, Integer> identifiers = new HashMap<>();
    private final List<UUID> ids = new ArrayList<>();
    private final Map<UUID, byte[]> bytes = new HashMap<>();
    private final Map<UUID, ReplayFrame.Actor> actors = new HashMap<>();

    ReplayPackedCodec() { this(null); }
    ReplayPackedCodec(ReplayStrings pool) { tail = new ReplayFrameCodec(pool); payload = new ReplayFrameCodec(pool); }

    long externalBytes() { return tail.externalBytes() + payload.externalBytes(); }

    void write(DataOutput out, ReplayFrame frame) throws IOException {
        out.writeInt(MAGIC);
        tail.write(out, new ReplayFrame(frame.tick(), List.of(), frame.blocks(), frame.scenes(), frame.mining(), frame.containers()));
        number(out, frame.actors().size());
        for (ReplayFrame.Actor actor : frame.actors()) {
            Integer id = identifiers.get(actor.id());
            if (id == null) {
                id = identifiers.size();
                identifiers.put(actor.id(), id);
                number(out, id);
                out.writeLong(actor.id().getMostSignificantBits());
                out.writeLong(actor.id().getLeastSignificantBits());
            } else number(out, id);
            if (actor.equals(actors.get(actor.id()))) {
                out.writeBoolean(false);
                continue;
            }
            out.writeBoolean(true);
            var buffer = new ByteArrayOutputStream();
            payload.write(new DataOutputStream(buffer), new ReplayFrame(0, List.of(actor), List.of(), List.of()));
            byte[] next = buffer.toByteArray();
            byte[] previous = bytes.getOrDefault(actor.id(), new byte[0]);
            int prefix = Arrays.mismatch(previous, next);
            if (prefix < 0) prefix = next.length;
            int suffix = 0;
            while (suffix < Math.min(previous.length, next.length) - prefix
                    && previous[previous.length - suffix - 1] == next[next.length - suffix - 1]) suffix++;
            number(out, prefix);
            number(out, suffix);
            number(out, next.length - prefix - suffix);
            out.write(next, prefix, next.length - prefix - suffix);
            bytes.put(actor.id(), next);
            actors.put(actor.id(), actor);
        }
    }

    ReplayFrame read(DataInput in) throws IOException {
        ReplayFrame frame = tail.read(in);
        int count = number(in);
        if (count > 1_000_000) throw new IOException("Invalid replay actor count");
        List<ReplayFrame.Actor> present = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int identifier = number(in);
            if (identifier > ids.size()) throw new IOException("Invalid replay actor reference");
            if (identifier == ids.size()) ids.add(new UUID(in.readLong(), in.readLong()));
            UUID id = ids.get(identifier);
            if (in.readBoolean()) {
                byte[] previous = bytes.getOrDefault(id, new byte[0]);
                int prefix = number(in), suffix = number(in), length = number(in);
                if ((long) prefix + suffix > previous.length || (long) prefix + suffix + length > 32 * 1024 * 1024) throw new IOException("Invalid replay actor delta");
                byte[] next = new byte[prefix + length + suffix];
                System.arraycopy(previous, 0, next, 0, prefix);
                in.readFully(next, prefix, length);
                System.arraycopy(previous, previous.length - suffix, next, prefix + length, suffix);
                try (var input = new DataInputStream(new ByteArrayInputStream(next))) {
                    ReplayFrame decoded = payload.read(input);
                    if (decoded.actors().size() != 1 || !decoded.actors().getFirst().id().equals(id) || input.read() != -1) throw new IOException("Invalid replay actor payload");
                    actors.put(id, decoded.actors().getFirst());
                }
                bytes.put(id, next);
            }
            ReplayFrame.Actor actor = actors.get(id);
            if (actor == null) throw new IOException("Missing replay actor definition");
            present.add(actor);
        }
        return new ReplayFrame(frame.tick(), present, frame.blocks(), frame.scenes(), frame.mining(), frame.containers());
    }

    private static void number(DataOutput out, int value) throws IOException {
        while ((value & ~127) != 0) { out.writeByte((value & 127) | 128); value >>>= 7; }
        out.writeByte(value);
    }

    private static int number(DataInput in) throws IOException {
        int value = 0;
        for (int shift = 0; shift < 35; shift += 7) {
            int next = in.readUnsignedByte();
            if (shift == 28 && (next & 248) != 0) throw new IOException("Invalid replay integer");
            value |= (next & 127) << shift;
            if ((next & 128) == 0) return value;
        }
        throw new IOException("Invalid replay integer");
    }
}
