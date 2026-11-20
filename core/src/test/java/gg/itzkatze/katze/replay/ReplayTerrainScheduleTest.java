package gg.itzkatze.katze.replay;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReplayTerrainScheduleTest {
    @Test void distributedPlayersDoNotTriggerFullTerrainRescansEverySecond() {
        UUID world = UUID.randomUUID();
        var chunks = new ArrayList<ReplayStore.ChunkKey>();
        for (int i = 0; i < 300; i++) chunks.add(new ReplayStore.ChunkKey(world, i, 0));
        var schedule = new ReplayTerrainSchedule();
        assertEquals(chunks, schedule.capture(chunks, 0, false, true));
        for (int tick = 1; tick < 200; tick++) assertTrue(schedule.capture(chunks, tick, false, true).isEmpty());
        var refreshed = new HashSet<ReplayStore.ChunkKey>();
        for (int tick = 200; tick < 350; tick++) {
            var selected = schedule.capture(chunks, tick, false, true);
            assertEquals(2, selected.size());
            refreshed.addAll(selected);
        }
        assertEquals(new HashSet<>(chunks), refreshed);
    }

    @Test void backpressureDefersRefreshesButNewTerrainAndFinalSnapshotsAreStillCaptured() {
        UUID world = UUID.randomUUID();
        var first = new ReplayStore.ChunkKey(world, 0, 0);
        var second = new ReplayStore.ChunkKey(world, 1, 0);
        var schedule = new ReplayTerrainSchedule();
        assertEquals(List.of(first), schedule.capture(List.of(first), 0, false, true));
        assertTrue(schedule.capture(List.of(first), 1000, false, false).isEmpty());
        assertEquals(List.of(second), schedule.capture(List.of(first, second), 1001, false, false));
        assertTrue(schedule.contains(second));
        assertEquals(List.of(first, second), schedule.capture(List.of(first, second), 1002, true, false));
    }
}
