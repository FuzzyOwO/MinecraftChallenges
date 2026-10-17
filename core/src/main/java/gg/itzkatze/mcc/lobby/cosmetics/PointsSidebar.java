package gg.itzkatze.mcc.lobby.cosmetics;

import gg.itzkatze.mcc.lobby.Lobby;
import gg.itzkatze.mcc.lobby.LobbyManager;
import gg.itzkatze.mcc.lobby.jumpandrun.JumpAndRun;
import gg.itzkatze.mcc.player.MccPlayer;
import gg.itzkatze.mcc.scoreboard.ScoreboardContent;
import gg.itzkatze.mcc.scoreboard.ScoreboardManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.OptionalInt;
import java.util.UUID;

public final class PointsSidebar implements ScoreboardContent {

    public static void install(@NotNull CosmeticStore store) {
        store.addBalanceListener(PointsSidebar::refresh);
    }

    public static void refresh(@NotNull UUID player) {
        MccPlayer.adapt(player).ifPresent(ScoreboardManager::refreshLobby);
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.text("Jump & Run");
    }

    @Override
    public @NotNull Component[] getLines(@NotNull MccPlayer player) {
        CosmeticStore store = CosmeticStore.get();
        UUID id = player.getUuid();
        OptionalInt score = currentRun(id);
        return new Component[]{
                Component.text(" "),
                stat("Current Run: ", score.isPresent() ? Component.text(score.getAsInt(), NamedTextColor.WHITE) : Component.text("-", NamedTextColor.DARK_GRAY)),
                stat("Best Run: ", Component.text(store.best(id), NamedTextColor.WHITE)),
                Component.text(" "),
                stat("Points: ", Component.text(store.points(id), NamedTextColor.GOLD)),
                stat("Lifetime Points: ", Component.text(store.earned(id), NamedTextColor.GOLD))
        };
    }

    private static @NotNull OptionalInt currentRun(@NotNull UUID player) {
        Lobby lobby = LobbyManager.getActiveLobby();
        if (lobby == null) return OptionalInt.empty();

        return lobby.getFeatures().stream()
                .filter(JumpAndRun.class::isInstance)
                .map(JumpAndRun.class::cast)
                .flatMapToInt(jumpAndRun -> jumpAndRun.score(player).stream())
                .findFirst();
    }

    private static @NotNull Component stat(@NotNull String label, @NotNull Component value) {
        return Component.text(label, NamedTextColor.GRAY).append(value);
    }
}
