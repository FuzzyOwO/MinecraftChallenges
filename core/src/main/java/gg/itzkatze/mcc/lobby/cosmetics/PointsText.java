package gg.itzkatze.mcc.lobby.cosmetics;

import org.jetbrains.annotations.NotNull;

public final class PointsText {

    private PointsText() {
    }

    public static @NotNull String amount(int points) {
        return points + (points == 1 ? " point" : " points");
    }

    public static @NotNull String missing(int points) {
        return "You need " + points + " more " + (points == 1 ? "point." : "points.");
    }
}
