package gg.itzkatze.mcc.lobby;

import gg.itzkatze.mcc.player.MccPlayer;
import org.jetbrains.annotations.NotNull;

public interface LobbyFeature {

    default void onCreate(@NotNull Lobby lobby) {
    }

    default void onDestroy(@NotNull Lobby lobby) {
    }

    default void onPlayerJoin(@NotNull MccPlayer player, @NotNull Lobby lobby) {
    }
}
