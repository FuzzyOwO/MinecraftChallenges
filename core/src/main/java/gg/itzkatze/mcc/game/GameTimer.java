package gg.itzkatze.mcc.game;

public interface GameTimer {

    enum Mode {
        DISABLED,
        COUNT_UP,
        COUNT_DOWN
    }

    int getStartTime();

    int getTime();

    boolean isRunning();

    Mode getMode();

    void setStartTime(int newStartTime);

    void setTime(int newTime);

    void setMode(Mode mode);

    boolean hasHitZero();

    void start();

    void pause();

    void stop();
}
