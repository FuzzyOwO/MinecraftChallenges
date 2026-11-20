package gg.itzkatze.katze.replay;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ReplayRecordingQueueTest {
    @Test void stalledWriterKeepsTheQueueBoundedAndCapturesTheLatestStateWhenItRecovers() {
        var jobs = new ArrayDeque<Runnable>();
        var queue = new ReplayRecordingQueue(jobs::addLast);
        var captured = new ArrayList<Integer>();
        var written = new ArrayList<Integer>();
        var latestBlockChange = new AtomicInteger();
        int skipped = 0;
        for (int tick = 0; tick < 2400; tick++) {
            int current = tick;
            latestBlockChange.set(tick);
            if (!queue.submit(() -> {
                captured.add(current);
                int blockChange = latestBlockChange.getAndSet(-1);
                return () -> written.add(blockChange);
            }, false)) skipped++;
        }
        assertEquals(8, jobs.size());
        assertEquals(8, captured.size());
        assertEquals(2392, skipped);
        assertEquals(2399, latestBlockChange.get());
        assertTrue(written.isEmpty());
        assertFalse(queue.canRefreshTerrain());

        while (!jobs.isEmpty()) jobs.removeFirst().run();
        assertTrue(queue.canRefreshTerrain());
        assertTrue(queue.submit(() -> {
            captured.add(2400);
            int blockChange = latestBlockChange.getAndSet(-1);
            return () -> written.add(blockChange);
        }, false));
        jobs.removeFirst().run();
        assertEquals(2400, captured.getLast());
        assertEquals(2399, written.getLast());
    }

    @Test void finalFrameIsSavedAfterQueuedFramesEvenWhenTheQueueIsFull() {
        var jobs = new ArrayDeque<Runnable>();
        var queue = new ReplayRecordingQueue(jobs::addLast);
        List<Integer> written = new ArrayList<>();
        for (int tick = 0; tick < 8; tick++) {
            int current = tick;
            assertTrue(queue.submit(() -> () -> written.add(current), false));
        }
        assertFalse(queue.submit(() -> fail("A skipped frame must not capture state"), false));
        assertTrue(queue.submit(() -> () -> written.add(2400), true));
        jobs.addLast(() -> written.add(-1));
        while (!jobs.isEmpty()) jobs.removeFirst().run();
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 2400, -1), written);
    }

    @Test void failedCaptureAndRejectedExecutionReleaseTheirReservations() {
        var queue = new ReplayRecordingQueue(Runnable::run);
        assertThrows(IllegalStateException.class, () -> queue.submit(() -> { throw new IllegalStateException(); }, false));
        assertThrows(IllegalStateException.class, () -> queue.submit(() -> () -> { throw new IllegalStateException(); }, false));
        assertTrue(queue.canRefreshTerrain());
        for (int i = 0; i < 20; i++) assertTrue(queue.submit(() -> null, false));
        assertTrue(queue.canRefreshTerrain());
        var rejected = new ReplayRecordingQueue(job -> { throw new RejectedExecutionException(); });
        assertThrows(RejectedExecutionException.class, () -> rejected.submit(() -> () -> {}, false));
        assertTrue(rejected.canRefreshTerrain());
    }
}
