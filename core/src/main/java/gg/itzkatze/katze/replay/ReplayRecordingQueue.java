package gg.itzkatze.katze.replay;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

final class ReplayRecordingQueue {
    private static final int MAX_PENDING = 8;
    private final Executor executor;
    private final AtomicInteger pending = new AtomicInteger();

    ReplayRecordingQueue(Executor executor) { this.executor = executor; }

    boolean submit(Supplier<Runnable> capture, boolean required) {
        int count;
        do {
            count = pending.get();
            if (!required && count >= MAX_PENDING) return false;
        } while (!pending.compareAndSet(count, count + 1));
        AtomicBoolean started = new AtomicBoolean();
        try {
            Runnable recording = capture.get();
            if (recording == null) { pending.decrementAndGet(); return true; }
            executor.execute(() -> {
                started.set(true);
                try { recording.run(); }
                finally { pending.decrementAndGet(); }
            });
            return true;
        } catch (RuntimeException | Error e) {
            if (!started.get()) pending.decrementAndGet();
            throw e;
        }
    }

    boolean canRefreshTerrain() { return pending.get() <= 2; }
}
