package gg.itzkatze.katze.replay;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ReplayPackedCodecTest {
    @Test void movingMobsAndFourPlayersCompressWithoutChangingAnyRecordedValue() throws Exception {
        List<ReplayFrame> frames = recording();
        byte[] legacy = encode(frames, false), packed = encode(frames, true);
        try (var in = new DataInputStream(ReplayCompression.read(new ByteArrayInputStream(packed)))) {
            var decoder = new ReplayFrameCodec();
            for (ReplayFrame frame : frames) assertEquals(frame, decoder.read(in));
            assertEquals(-1, in.read());
        }
        System.out.println("Lossless replay benchmark: legacy=" + legacy.length + " bytes, packed=" + packed.length + " bytes, reduction="
                + (100 - 100L * packed.length / legacy.length) + "%");
        assertTrue(packed.length < legacy.length, "Packed: " + packed.length + ", legacy: " + legacy.length);
    }

    @Test void disappearingAndReturningActorsAndEmptyFramesRoundTrip() throws Exception {
        List<ReplayFrame> original = recording().subList(0, 4);
        List<ReplayFrame> frames = List.of(original.get(0), new ReplayFrame(1, List.of(), List.of(), List.of()),
                new ReplayFrame(2, original.get(0).actors().reversed(), List.of(), List.of()), original.get(3));
        try (var in = new DataInputStream(ReplayCompression.read(new ByteArrayInputStream(encode(frames, true))))) {
            var decoder = new ReplayFrameCodec();
            for (ReplayFrame frame : frames) assertEquals(frame, decoder.read(in));
        }
    }

    private byte[] encode(List<ReplayFrame> frames, boolean packed) throws Exception {
        var bytes = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(packed ? ReplayCompression.write(bytes) : new GZIPOutputStream(bytes))) {
            var legacy = new ReplayFrameCodec();
            var next = new ReplayPackedCodec();
            for (ReplayFrame frame : frames) {
                if (packed) next.write(out, frame);
                else legacy.write(out, frame);
            }
        }
        return bytes.toByteArray();
    }

    private List<ReplayFrame> recording() {
        var random = new Random(42);
        UUID world = new UUID(0, 1);
        List<String> inventory = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            byte[] item = new byte[200]; random.nextBytes(item);
            inventory.add(Base64.getEncoder().encodeToString(item));
        }
        List<ReplayFrame> frames = new ArrayList<>();
        double[] x = new double[204], z = new double[204];
        for (int tick = 0; tick < 100; tick++) {
            List<ReplayFrame.Actor> actors = new ArrayList<>();
            for (int actor = 0; actor < 204; actor++) {
                boolean moving = actor < 104;
                if (moving) { x[actor] += random.nextDouble(-0.2, 0.2); z[actor] += random.nextDouble(-0.2, 0.2); }
                boolean player = actor < 4;
                var state = player && tick % 10 == 0 ? new ReplayFrame.PlayerState(inventory, inventory.subList(0, 9), actor,
                        20 - tick % 3, 20, 20 - tick % 4, 7, "arena", List.of("information"), Map.of("MOB_ARMY", List.of("Wave 1", "Mobs " + (200 - tick)))) : null;
                actors.add(new ReplayFrame.Actor(new UUID(1, actor), world, player ? "PLAYER" : "ZOMBIE", player ? "player-" + actor : null,
                        tick == 0 && !player ? "entity-snapshot-" + actor : null, tick == 0 && player ? "skin" : null, null,
                        x[actor], 90, z[actor], moving ? tick * 0.7f : 0, 0, "STANDING", false, tick % 30 == 0,
                        tick == 0 ? inventory.subList(0, 6) : null, null, tick % 25 == 0, 0, null, state));
            }
            frames.add(new ReplayFrame(tick, actors, List.of(), List.of(new ReplayFrame.Scene(world, "NORMAL", 6000, false, false))));
        }
        return frames;
    }
}
