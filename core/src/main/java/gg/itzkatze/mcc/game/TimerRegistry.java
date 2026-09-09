package gg.itzkatze.mcc.game;

import lombok.Getter;
import lombok.Setter;

public final class TimerRegistry {

    @Getter
    @Setter
    private static GameTimer timer = new NoOpTimer();

    private TimerRegistry() {
    }

    public static void resetToNoOp() {
        timer = new NoOpTimer();
    }

    private static final class NoOpTimer implements GameTimer {
        private int startTime = 5400;
        private int time = 0;
        private boolean running = false;
        private Mode mode = Mode.DISABLED;

        @Override public int getStartTime() { return startTime; }
        @Override public int getTime() { return time; }
        @Override public boolean isRunning() { return running; }
        @Override public Mode getMode() { return mode; }
        @Override public void setStartTime(int newStartTime) { this.startTime = newStartTime; }
        @Override public void setTime(int newTime) { this.time = newTime; }
        @Override public void setMode(Mode mode) { this.mode = mode; }
        @Override public boolean hasHitZero() { return false; }
        @Override public void start() { this.running = true; }
        @Override public void pause() { this.running = false; }
        @Override public void stop() { this.running = false; }

    }
}
