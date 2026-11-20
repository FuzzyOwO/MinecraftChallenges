package gg.itzkatze.katze.replay;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReplayCheckpointTest {
    @TempDir Path directory;
    private final UUID player = UUID.randomUUID(), overworld = UUID.randomUUID(), arena = UUID.randomUUID();

    @Test void checkpointPayloadsCanShareTheArchiveStringPoolWhileRecordingContinues() throws Exception {
        Path path = directory.resolve("frames.bin");
        List<ReplayFrame> frames = new ArrayList<>();
        for (int tick = 0; tick <= 40; tick += 20) {
            var state = new ReplayFrame.PlayerState(List.of(("item-" + tick).repeat(100)), null, 1, 20, 20, 20, 0, "arena", List.of());
            var actor = new ReplayFrame.Actor(player, arena, "PLAYER", "Katze", null, "skin".repeat(100), null,
                    0, 90, 0, 0, 0, "STANDING", false, false, List.of(), null, false, 0, null, state);
            frames.add(new ReplayFrame(tick, List.of(actor), List.of(), List.of()));
        }
        var builder = new ReplayCheckpoint.Builder();
        try (var writer = new ReplayArchive.Writer(path, false)) {
            for (var frame : frames.subList(0, 2)) { writer.write(frame); builder.add(frame); }
            builder.build().write(directory.resolve("combat.bin"));
            writer.write(frames.getLast());
        }
        var archive = ReplayArchive.open(path, new ReplayStore.Info(4, "26.3", UUID.randomUUID(), List.of(player), 40, 20));
        assertEquals(frames, archive);
        assertEquals(frames.get(1), ReplayCheckpoint.read(directory.resolve("combat.bin")).frame());
    }

    @Test void combatJumpDoesNotReadCollectionFramesOrApplyTerrainInHiddenWorlds() throws Exception {
        var frames = frames();
        var builder = new ReplayCheckpoint.Builder();
        frames.forEach(builder::add);
        var checkpoint = builder.build();
        Path file = directory.resolve("combat.bin");
        checkpoint.write(file);
        assertEquals(checkpoint, ReplayCheckpoint.read(file));
        List<ReplayFrame> guarded = new AbstractList<>() {
            @Override public ReplayFrame get(int index) { throw new AssertionError("Combat jump read frame " + index); }
            @Override public int size() { return frames.size(); }
        };
        var source = new ReplayTimeline(frames, 108_000);
        source.moveTo(0, (block, reverse) -> fail());
        var physical = new ReplayTerrainState();
        var seek = ReplaySeek.prepare(source, 108_000, physical, checkpoint, guarded, 108_000);
        assertTrue(seek.apply(128, Long.MAX_VALUE, () -> 0, block -> fail("Hidden terrain updated")));
        assertEquals(108_000, seek.timeline().position());
        assertEquals(arena, seek.timeline().currentFrame().actors().getFirst().world());
        assertEquals("skin", seek.timeline().definition(player).texture());
        assertEquals(List.of("sword"), seek.timeline().equipment(player));
        assertEquals(1, seek.deferredWorlds().size());
        assertTrue(seek.deferredWorlds().contains(overworld));
    }

    @Test void rewindingOutOfCombatRestoresAllOriginalStateAndTerrain() {
        var frames = frames();
        var builder = new ReplayCheckpoint.Builder(); frames.forEach(builder::add);
        var timeline = new ReplayTimeline(frames, 108_000, builder.build());
        List<ReplayFrame.BlockChange> reversed = new ArrayList<>();
        timeline.moveTo(0, (block, reverse) -> reversed.add(block));
        assertEquals(1, reversed.size());
        assertEquals("stone", reversed.getFirst().before());
        assertEquals("air", reversed.getFirst().after());
        assertEquals(overworld, timeline.currentFrame().actors().getFirst().world());
        assertEquals("skin", timeline.definition(player).texture());
        assertEquals(List.of("sword"), timeline.equipment(player));
        timeline.moveTo(108_000, (block, reverse) -> {});
        assertEquals(arena, timeline.currentFrame().actors().getFirst().world());
    }

    private List<ReplayFrame> frames() {
        var first = actor(overworld, "skin", List.of("sword"));
        var last = actor(arena, null, null);
        return List.of(new ReplayFrame(0, List.of(first), List.of(), List.of()),
                new ReplayFrame(20, List.of(first), List.of(new ReplayFrame.BlockChange(overworld, 0, 90, 0, "air", "stone")), List.of()),
                new ReplayFrame(108_000, List.of(last), List.of(), List.of()));
    }

    private ReplayFrame.Actor actor(UUID world, String skin, List<String> equipment) {
        return new ReplayFrame.Actor(player, world, "PLAYER", "Katze", null, skin, null,
                0, 90, 0, 0, 0, "STANDING", false, false, equipment);
    }
}
