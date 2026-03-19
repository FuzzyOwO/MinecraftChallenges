package gg.itzkatze.mcc.player;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface PlayerAdapterFactory {

    /**
     * Creates a challenge-specific player wrapper for the given Bukkit player.
     * @param player The Bukkit player
     * @return The wrapped player, or null if this factory does not handle this player
     */
    @NotNull MccPlayer adapt(@NotNull Player player);
}
