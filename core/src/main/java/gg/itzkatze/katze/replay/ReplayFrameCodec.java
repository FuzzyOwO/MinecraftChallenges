package gg.itzkatze.katze.replay;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class ReplayFrameCodec {
    private final List<String> strings = new ArrayList<>();
    private final Map<String, Integer> indices = new HashMap<>();
    private ReplayPackedCodec packed;
    private final ReplayStrings pool;
    private final Map<Integer, String> external = new HashMap<>();
    private long externalBytes;

    ReplayFrameCodec() { this(null); }
    ReplayFrameCodec(ReplayStrings pool) { this.pool = pool; }

    long externalBytes() { return externalBytes; }

    void write(DataOutput out, ReplayFrame frame) throws IOException {
        out.writeInt(frame.tick() | Integer.MIN_VALUE);
        out.writeInt(frame.actors().size());
        for (var actor : frame.actors()) {
            uuid(out, actor.id()); uuid(out, actor.world());
            text(out, actor.type()); text(out, actor.name()); text(out, actor.snapshot());
            text(out, actor.texture()); text(out, actor.signature());
            out.writeDouble(actor.x()); out.writeDouble(actor.y()); out.writeDouble(actor.z());
            out.writeFloat(actor.yaw()); out.writeFloat(actor.pitch()); text(out, actor.pose());
            out.writeBoolean(actor.burning()); out.writeBoolean(actor.swinging()); texts(out, actor.equipment());
            uuid(out, actor.vehicle()); out.writeBoolean(actor.hurt()); out.writeFloat(actor.hurtDirection()); text(out, actor.item());
            var player = actor.player();
            out.writeBoolean(player != null);
            if (player != null) {
                texts(out, player.inventory()); texts(out, player.backpack());
                out.writeBoolean(player.team() != null);
                if (player.team() != null) out.writeInt(player.team());
                out.writeDouble(player.health()); out.writeDouble(player.maxHealth());
                out.writeInt(player.food()); out.writeInt(player.level()); text(out, player.world());
                texts(out, player.information());
                out.writeInt(player.scoreboards().size());
                for (var entry : player.scoreboards().entrySet()) { text(out, entry.getKey()); texts(out, entry.getValue()); }
            }
            out.writeDouble(actor.health());
            out.writeDouble(actor.maxHealth());
        }
        out.writeInt(frame.blocks().size());
        for (var block : frame.blocks()) {
            uuid(out, block.world()); out.writeInt(block.x()); out.writeInt(block.y()); out.writeInt(block.z());
            text(out, block.before()); text(out, block.after());
        }
        out.writeInt(frame.scenes().size());
        for (var scene : frame.scenes()) {
            uuid(out, scene.world()); text(out, scene.environment()); out.writeLong(scene.time());
            out.writeBoolean(scene.storm()); out.writeBoolean(scene.thunder());
        }
        out.writeInt(frame.mining().size());
        for (var mining : frame.mining()) {
            uuid(out, mining.player()); uuid(out, mining.world());
            out.writeInt(mining.x()); out.writeInt(mining.y()); out.writeInt(mining.z()); out.writeFloat(mining.progress());
        }
        out.writeInt(frame.containers().size());
        for (var container : frame.containers()) {
            uuid(out, container.world()); out.writeInt(container.x()); out.writeInt(container.y()); out.writeInt(container.z());
        }
    }

    ReplayFrame read(DataInput in) throws IOException {
        int encodedTick = in.readInt();
        if (encodedTick == ReplayPackedCodec.MAGIC) {
            if (packed == null) packed = new ReplayPackedCodec(pool);
            return packed.read(in);
        }
        boolean hasContainers = encodedTick < 0;
        int tick = encodedTick & Integer.MAX_VALUE;
        List<ReplayFrame.Actor> actors = new ArrayList<>();
        int count = count(in);
        for (int i = 0; i < count; i++) {
            UUID id = uuid(in), world = uuid(in);
            String type = text(in), name = text(in), snapshot = text(in), texture = text(in), signature = text(in);
            double x = in.readDouble(), y = in.readDouble(), z = in.readDouble();
            float yaw = in.readFloat(), pitch = in.readFloat();
            String pose = text(in);
            boolean burning = in.readBoolean(), swinging = in.readBoolean();
            List<String> equipment = texts(in);
            UUID vehicle = uuid(in);
            boolean hurt = in.readBoolean();
            float direction = in.readFloat();
            String item = text(in);
            ReplayFrame.PlayerState player = null;
            double health = 0;
            double maximum = 0;
            if (in.readBoolean()) {
                List<String> inventory = texts(in), backpack = texts(in);
                Integer team = in.readBoolean() ? in.readInt() : null;
                health = in.readDouble();
                maximum = in.readDouble();
                int food = in.readInt(), level = in.readInt();
                String dimension = text(in);
                List<String> information = texts(in);
                Map<String, List<String>> scoreboards = new HashMap<>();
                int boards = count(in);
                for (int j = 0; j < boards; j++) scoreboards.put(text(in), texts(in));
                player = new ReplayFrame.PlayerState(inventory, backpack, team, health, maximum, food, level, dimension, information, scoreboards);
            }
            actors.add(new ReplayFrame.Actor(
                    id, world, type, name, snapshot, texture, signature, x, y, z, yaw, pitch, pose,
                    burning, swinging, equipment, vehicle, hurt, direction, item, player, health, maximum
            ));
        }
        List<ReplayFrame.BlockChange> blocks = new ArrayList<>();
        count = count(in);
        for (int i = 0; i < count; i++) blocks.add(new ReplayFrame.BlockChange(uuid(in), in.readInt(), in.readInt(), in.readInt(), text(in), text(in)));
        List<ReplayFrame.Scene> scenes = new ArrayList<>();
        count = count(in);
        for (int i = 0; i < count; i++) scenes.add(new ReplayFrame.Scene(uuid(in), text(in), in.readLong(), in.readBoolean(), in.readBoolean()));
        List<ReplayFrame.Mining> mining = new ArrayList<>();
        count = count(in);
        for (int i = 0; i < count; i++) mining.add(new ReplayFrame.Mining(uuid(in), uuid(in), in.readInt(), in.readInt(), in.readInt(), in.readFloat()));
        List<ReplayFrame.Container> containers = new ArrayList<>();
        if (hasContainers) {
            count = count(in);
            for (int i = 0; i < count; i++) containers.add(new ReplayFrame.Container(uuid(in), in.readInt(), in.readInt(), in.readInt()));
        }
        return new ReplayFrame(tick, actors, blocks, scenes, mining, containers);
    }

    private void text(DataOutput out, String value) throws IOException {
        if (value == null) { out.writeInt(-1); return; }
        if (pool != null && value.length() >= 128) {
            int id = pool.id(value);
            if (external.putIfAbsent(id, value) == null) externalBytes += value.length();
            out.writeInt(-3); out.writeInt(id); return;
        }
        Integer index = indices.get(value);
        if (index != null) { out.writeInt(index); return; }
        out.writeInt(-2);
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length); out.write(bytes);
        indices.put(value, indices.size());
    }

    private String text(DataInput in) throws IOException {
        int index = in.readInt();
        if (index == -1) return null;
        if (index == -3 && pool != null) {
            int id = in.readInt();
            String value = external.get(id);
            if (value == null) { value = pool.get(id); external.put(id, value); }
            return value;
        }
        if (index >= 0 && index < strings.size()) return strings.get(index);
        if (index != -2) throw new IOException("Invalid replay string reference");
        int length = in.readInt();
        if (length < 0 || length > 16 * 1024 * 1024) throw new IOException("Invalid replay string size");
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        String value = new String(bytes, StandardCharsets.UTF_8);
        strings.add(value);
        return value;
    }

    private void texts(DataOutput out, List<String> values) throws IOException {
        out.writeInt(values == null ? -1 : values.size());
        if (values != null) for (String value : values) text(out, value);
    }

    private List<String> texts(DataInput in) throws IOException {
        int size = in.readInt();
        if (size == -1) return null;
        checkCount(size);
        List<String> values = new ArrayList<>();
        for (int i = 0; i < size; i++) values.add(text(in));
        return List.copyOf(values);
    }

    private void uuid(DataOutput out, UUID id) throws IOException {
        out.writeBoolean(id != null);
        if (id != null) { out.writeLong(id.getMostSignificantBits()); out.writeLong(id.getLeastSignificantBits()); }
    }

    private UUID uuid(DataInput in) throws IOException { return in.readBoolean() ? new UUID(in.readLong(), in.readLong()) : null; }
    private int count(DataInput in) throws IOException { int count = in.readInt(); checkCount(count); return count; }
    private void checkCount(int count) throws IOException { if (count < 0 || count > 1_000_000) throw new IOException("Invalid replay collection size"); }
}
