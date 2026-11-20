package gg.itzkatze.katze.replay;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.AbstractList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ReplayTimelineTest {
    private final UUID player = UUID.randomUUID();
    private final UUID world = UUID.randomUUID();

    @Test void aLateRewindReadsPriorStateVersionsWithoutRescanningTheRecording() {
        var initial = new ReplayFrame.PlayerState(List.of("sword"), null, null, 20, 20, 20, 0, "game", List.of());
        var later = new ReplayFrame.PlayerState(List.of("pickaxe"), null, null, 10, 20, 20, 0, "game", List.of());
        var frames = IntStream.rangeClosed(0, 20_000).mapToObj(tick -> {
            var actor = new ReplayFrame.Actor(player, world, "PLAYER", "Katze", null, tick == 0 ? "texture" : null, null,
                    0, 64, 0, 0, 0, "STANDING", false, false,
                    tick == 0 ? List.of("sword") : tick == 19_995 ? List.of("pickaxe") : null,
                    null, false, 0, tick == 0 ? "apple" : tick == 19_995 ? "bread" : null,
                    tick == 0 ? initial : tick == 10_000 ? later : tick == 19_995 ? initial : null);
            return frame(tick, actor, List.of());
        }).toList();
        AtomicInteger reads = new AtomicInteger();
        List<ReplayFrame> counted = new AbstractList<>() {
            @Override public ReplayFrame get(int index) { reads.incrementAndGet(); return frames.get(index); }
            @Override public int size() { return frames.size(); }
        };
        var source = new ReplayTimeline(counted, 20_000);
        source.moveTo(20_000, (block, reverse) -> fail());
        reads.set(0);
        var rewind = ReplaySeek.prepare(source, 19_990).timeline();
        assertTrue(reads.get() < 50, "Frame reads: " + reads.get());
        assertEquals(later, rewind.player(player));
        assertEquals(List.of("sword"), rewind.equipment(player));
        assertEquals("apple", rewind.item(player));
        assertEquals("texture", rewind.definition(player).texture());
        assertEquals(initial, source.player(player));
        rewind.moveTo(20_000, (block, reverse) -> fail());
        rewind.moveTo(19_990, (block, reverse) -> fail());
        assertEquals(later, rewind.player(player));
        assertEquals(List.of("sword"), rewind.equipment(player));
    }

    @Test void rewindingBeforeAnActorAppearsRemovesItsDefinitionAndState() {
        var timeline = new ReplayTimeline(List.of(new ReplayFrame(0, List.of(), List.of(), List.of()),
                frame(10, actor("texture", List.of("sword")), List.of())), 20);
        timeline.moveTo(20, (block, reverse) -> fail());
        timeline.moveTo(0, (block, reverse) -> fail());
        assertNull(timeline.definition(player));
        assertNull(timeline.equipment(player));
        assertTrue(timeline.players().isEmpty());
        timeline.moveTo(20, (block, reverse) -> fail());
        assertEquals("texture", timeline.definition(player).texture());
        assertEquals(List.of("sword"), timeline.equipment(player));
    }

    @Test void aShortRewindLateInTheReplayOnlyRevertsTheCrossedTerrain() {
        List<ReplayFrame> frames = IntStream.rangeClosed(0, 20_000)
                .mapToObj(tick -> frame(tick, actor(tick == 0 ? "texture" : null, tick == 0 ? List.of("sword") : null),
                        tick == 0 ? List.of() : List.of(block("state-" + (tick - 1), "state-" + tick)))).toList();
        var timeline = new ReplayTimeline(frames, 20_000);
        timeline.moveTo(20_000, (block, reverse) -> {});
        List<String> changed = new ArrayList<>();
        timeline.moveTo(19_990, (block, reverse) -> {
            assertTrue(reverse);
            changed.add(block.before());
        });
        assertEquals(10, changed.size());
        assertEquals("state-19990", changed.getLast());
        assertEquals("texture", timeline.definition(player).texture());
        assertEquals(List.of("sword"), timeline.equipment(player));
        assertEquals(19_990, timeline.currentFrame().tick());
    }

    @Test
    void recordedInventoryAndMiningStateFollowThePlayheadAndRestoreOnRewind() {
        var initial = new ReplayFrame.PlayerState(List.of("sword"), List.of("apple"), 2, 20, 20, 20, 3, "game", List.of());
        var changed = new ReplayFrame.PlayerState(List.of("pickaxe"), List.of(), 2, 8, 20, 18, 4, "game", List.of());
        var mining = new ReplayFrame.Mining(player, world, 0, 64, 0, 0.5f);
        var first = new ReplayFrame.Actor(player, world, "PLAYER", "Katze", null, null, null, 0, 64, 0, 0, 0,
                "STANDING", false, false, List.of(), null, false, 0, null, initial);
        var later = new ReplayFrame.Actor(player, world, "PLAYER", "Katze", null, null, null, 1, 64, 0, 0, 0,
                "STANDING", false, false, null, null, true, 45, null, changed);
        var timeline = new ReplayTimeline(List.of(new ReplayFrame(0, List.of(first), List.of(), List.of(), List.of(mining)),
                new ReplayFrame(1, List.of(later), List.of(), List.of())), 2);
        timeline.moveTo(1, (block, reverse) -> fail());
        assertEquals(changed, timeline.player(player));
        assertTrue(timeline.currentFrame().mining().isEmpty());
        timeline.moveTo(0, (block, reverse) -> fail());
        assertEquals(initial, timeline.player(player));
        assertEquals(List.of(mining), timeline.currentFrame().mining());
    }

    @Test
    void rewindRestoresTerrainEquipmentAndPlayerDefinitions() {
        var placed = block("minecraft:air", "minecraft:stone");
        var replaced = block("minecraft:stone", "minecraft:gold_block");
        var timeline = new ReplayTimeline(List.of(
                frame(0, actor("texture", List.of("sword")), List.of()),
                frame(20, actor(null, List.of("pickaxe")), List.of(placed)),
                frame(40, actor(null, null), List.of(replaced))), 60);
        List<String> terrain = new ArrayList<>();
        timeline.moveTo(60, (block, reverse) -> terrain.add(reverse ? block.before() : block.after()));
        assertEquals(List.of("minecraft:stone", "minecraft:gold_block"), terrain);
        assertEquals(List.of("pickaxe"), timeline.equipment(player));
        assertEquals("texture", timeline.definition(player).texture());

        terrain.clear();
        ReplayFrame atStart = timeline.moveTo(0, (block, reverse) -> terrain.add(reverse ? block.before() : block.after()));
        assertEquals(0, atStart.tick());
        assertEquals(List.of("minecraft:stone", "minecraft:air"), terrain);
        assertEquals(List.of("sword"), timeline.equipment(player));
        assertEquals("texture", timeline.definition(player).texture());

        terrain.clear();
        timeline.moveTo(20, (block, reverse) -> terrain.add(reverse ? block.before() : block.after()));
        assertEquals(List.of("minecraft:stone"), terrain);
        assertEquals(List.of("pickaxe"), timeline.equipment(player));
    }

    @Test
    void seekingAcrossAWorldChangeKeepsTheRecordedDimensionAndDespawn() {
        UUID nether = UUID.randomUUID();
        var first = actor("texture", List.of(""));
        var moved = new ReplayFrame.Actor(player, nether, "PLAYER", "Katze", null, null, null,
                12, 70, -8, 90, 10, "STANDING", false, false, null);
        var timeline = new ReplayTimeline(List.of(frame(0, first, List.of()), frame(2, moved, List.of()),
                new ReplayFrame(4, List.of(), List.of(), List.of())), 6);
        assertEquals(nether, timeline.moveTo(3, (block, reverse) -> fail()).actors().getFirst().world());
        assertTrue(timeline.moveTo(6, (block, reverse) -> fail()).actors().isEmpty());
        assertEquals(world, timeline.moveTo(0, (block, reverse) -> fail()).actors().getFirst().world());
    }

    @Test
    void fractionalPlaybackWaitsForTheNextFrameAndClampsSeekBounds() {
        var timeline = new ReplayTimeline(List.of(frame(0, actor(null, List.of()), List.of()), frame(2, actor(null, null), List.of())), 4);
        assertEquals(0, timeline.moveTo(-10, (block, reverse) -> fail()).tick());
        assertNull(timeline.moveTo(0.25, (block, reverse) -> fail()));
        assertNull(timeline.moveTo(1.5, (block, reverse) -> fail()));
        assertEquals(2, timeline.moveTo(10, (block, reverse) -> fail()).tick());
        assertEquals(4, timeline.position());
    }

    @Test
    void rejectsOutOfOrderFrames() {
        assertThrows(IllegalArgumentException.class, () -> new ReplayTimeline(List.of(frame(2, actor(null, null), List.of())), 4));
        assertThrows(IllegalArgumentException.class, () -> new ReplayTimeline(List.of(frame(0, actor(null, null), List.of()), frame(0, actor(null, null), List.of())), 4));
    }

    private ReplayFrame frame(int tick, ReplayFrame.Actor actor, List<ReplayFrame.BlockChange> blocks) {
        return new ReplayFrame(tick, List.of(actor), blocks, List.of());
    }

    private ReplayFrame.Actor actor(String texture, List<String> equipment) {
        return new ReplayFrame.Actor(player, world, "PLAYER", "Katze", null, texture, null,
                0, 64, 0, 0, 0, "STANDING", false, false, equipment);
    }

    private ReplayFrame.BlockChange block(String before, String after) {
        return new ReplayFrame.BlockChange(world, -1, -63, 31, before, after);
    }
}
