package gg.itzkatze.mcc.lobby.cosmetics;

import gg.itzkatze.mcc.player.MccPlayer;
import gg.itzkatze.mcc.scoreboard.ScoreboardContent;
import gg.itzkatze.mcc.scoreboard.ScoreboardManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

public final class PointsSidebar implements ScoreboardContent {

    public static void install(@NotNull CosmeticStore store) {
        store.addBalanceListener(player -> MccPlayer.adapt(player).ifPresent(ScoreboardManager::refreshLobby));
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.text("Jump & Run");
    }

    @Override
    public @NotNull Component[] getLines(@NotNull MccPlayer player) {
        return new Component[]{
                Component.text(" "),
                Component.text("Points: ", NamedTextColor.GRAY).append(Component.text(CosmeticStore.get().points(player.getUuid()), NamedTextColor.GOLD))
        };
    }
}
