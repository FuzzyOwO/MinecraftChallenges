package gg.itzkatze.mcc.challenges;

import gg.itzkatze.mcc.game.GameTimer;

public record TimerSpec(GameTimer.Mode mode, int startSeconds) {

    public TimerSpec {
        if (startSeconds < 0) {
            throw new IllegalArgumentException("startSeconds cannot be negative");
        }
    }
}
