package gg.itzkatze.katze.replay;

final class ReplayTransport {
    private boolean ticked;
    private int lastTick;
    private boolean paused;
    private double speed = 1;

    boolean claimTick(int tick) {
        if (ticked && lastTick == tick) return false;
        ticked = true;
        lastTick = tick;
        return true;
    }

    boolean paused() { return paused; }
    double speed() { return speed; }
    void pause() { paused = true; }
    void togglePause() { paused = !paused; }
    void cycleSpeed() { speed = speed == 4 ? 0.25 : speed * 2; }
}
