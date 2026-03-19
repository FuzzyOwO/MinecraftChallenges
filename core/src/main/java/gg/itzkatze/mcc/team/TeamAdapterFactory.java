package gg.itzkatze.mcc.team;

import gg.itzkatze.mcc.player.MccPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Factory for challenge-specific team wrappers.
 * Challenge modules register their factory in {@link MccTeam#registerFactory} when loaded.
 */
@FunctionalInterface
public interface TeamAdapterFactory {

    /**
     * Creates a challenge-specific team wrapper for the given players.
     * @param players The team members
     * @param id The team id to keep consistent across upgrades
     * @param register Whether to register the team in {@link MccTeam#teams}
     * @return The wrapped team
     */
    @NotNull MccTeam adapt(@NotNull List<MccPlayer> players, int id, boolean register);
}
