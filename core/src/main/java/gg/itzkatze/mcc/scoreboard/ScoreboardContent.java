package gg.itzkatze.mcc.scoreboard;

import gg.itzkatze.mcc.player.MccPlayer;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public interface ScoreboardContent {

    /**
     * Gets the title to display on the scoreboard.
     * @return The scoreboard title
     */
    @NotNull Component getTitle();

    /**
     * Gets the scoreboard lines for a specific player.
     * @param player The player to get scoreboard content for
     * @return Array of scoreboard lines (max 15 lines, index 0 is top)
     */
    @NotNull Component[] getLines(@NotNull MccPlayer player);

    /**
     * Gets the priority of this scoreboard content when multiple challenges are active.
     * Higher priority content appears first.
     * @return The priority (higher = appears first)
     */
    default int getPriority() {
        return 0;
    }
}
