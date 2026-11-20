package gg.itzkatze.katze.replay;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ReplayFrameCompatibilityTest {
    @Test void olderFramesDefaultToNoNewEffectsAndRemainPlayable() {
        UUID id = UUID.randomUUID(), world = UUID.randomUUID();
        var frame = new Gson().fromJson("""
                {"tick":0,"actors":[{"id":"%s","world":"%s","type":"PLAYER","name":"Katze","x":0,"y":64,"z":0,
                "yaw":0,"pitch":0,"pose":"STANDING","burning":false,"swinging":false,"equipment":[]}],"blocks":[],"scenes":[]}
                """.formatted(id, world), ReplayFrame.class);
        assertTrue(frame.mining().isEmpty());
        assertTrue(frame.containers().isEmpty());
        assertFalse(frame.actors().getFirst().hurt());
        assertNull(frame.actors().getFirst().vehicle());
        assertNull(frame.actors().getFirst().player());
        var timeline = new ReplayTimeline(List.of(frame), 2);
        assertEquals(frame, timeline.moveTo(0, (block, backwards) -> fail()));
    }

    @Test void vehicleHurtInventoryAndMiningDataSurviveSerializationTogether() {
        UUID id = UUID.randomUUID(), world = UUID.randomUUID(), boat = UUID.randomUUID();
        var inventory = new ReplayFrame.PlayerState(List.of("enchanted-item", ""), List.of("backpack-item"), 2, 12, 20, 18, 5, "game", List.of());
        var actor = new ReplayFrame.Actor(id, world, "PLAYER", "Katze", null, "skin", "signature", 0, 64, 0, 0, 0,
                "STANDING", false, true, List.of("hand-item"), boat, true, 90, null, inventory);
        var frame = new ReplayFrame(0, List.of(actor), List.of(), List.of(), List.of(new ReplayFrame.Mining(id, world, 0, 64, 0, 0.4f)),
                List.of(new ReplayFrame.Container(world, 1, 64, 0)));
        Gson gson = new Gson();
        assertEquals(frame, gson.fromJson(gson.toJson(frame), ReplayFrame.class));
    }

    @Test void binaryFramesFromBeforeContainerRecordingRemainReadable() throws Exception {
        var frame = new ReplayFrame(123, List.of(), List.of(), List.of(), List.of());
        var bytes = new ByteArrayOutputStream();
        new ReplayFrameCodec().write(new DataOutputStream(bytes), frame);
        byte[] older = Arrays.copyOf(bytes.toByteArray(), bytes.size() - Integer.BYTES);
        ByteBuffer.wrap(older).putInt(frame.tick());
        assertEquals(frame, new ReplayFrameCodec().read(new DataInputStream(new ByteArrayInputStream(older))));
    }
}
