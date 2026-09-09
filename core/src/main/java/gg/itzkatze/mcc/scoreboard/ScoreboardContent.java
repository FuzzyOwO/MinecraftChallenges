package gg.itzkatze.mcc.scoreboard;

import gg.itzkatze.mcc.player.MccPlayer;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public interface ScoreboardContent {

    @NotNull Component getTitle();

    @NotNull Component[] getLines(@NotNull MccPlayer player);

    // Higher priority content appears first.
    default int getPriority() {
        return 0;
    }
}
