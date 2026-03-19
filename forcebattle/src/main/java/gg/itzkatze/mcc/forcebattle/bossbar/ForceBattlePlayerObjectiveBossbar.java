package gg.itzkatze.mcc.forcebattle.bossbar;

import gg.itzkatze.mcc.bossbar.BossbarContent;
import gg.itzkatze.mcc.bossbar.BossbarKind;
import gg.itzkatze.mcc.forcebattle.ForceBattleObjective;
import gg.itzkatze.mcc.configuration.GlobalSetting;
import gg.itzkatze.mcc.configuration.SettingsManager;
import gg.itzkatze.mcc.forcebattle.ForceBattlePlayer;
import gg.itzkatze.mcc.player.MccPlayer;
import gg.itzkatze.mcc.util.StringUtility;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ForceBattlePlayerObjectiveBossbar implements BossbarContent {

    @Override
    @NotNull public BossbarKind getKind() {
        return BossbarKind.RUNNING;
    }

    @Override
    @Nullable public BossBar getBossbar(@NotNull MccPlayer mccPlayer) {
        ForceBattlePlayer player = (ForceBattlePlayer) mccPlayer;
        ForceBattleObjective objective = player.getObjective();
        if (objective == null) return null;

        return BossBar.bossBar(
                getTitle(player),
                1,
                BossBar.Color.BLUE,
                BossBar.Overlay.PROGRESS
        );
    }

    private @NotNull Component getTitle(MccPlayer mccPlayer) {
        ForceBattlePlayer player = (ForceBattlePlayer) mccPlayer;
        ForceBattleObjective playerObjective = player.getObjective();

        if (playerObjective == null) return Component.empty();

        boolean isPlayerInTeam = mccPlayer.isInTeam();
        int points = isPlayerInTeam ? mccPlayer.getTeam().getPoints() : mccPlayer.getPoints();

        if (SettingsManager.isEnabled(GlobalSetting.HIDE_POINTS)) {
            return Component.text(playerObjective.getBattleType().getPrefix(), NamedTextColor.GRAY)
                    .append(Component.text(": ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(StringUtility.formatName(playerObjective.getObjectiveString()), NamedTextColor.BLUE));
        }

        return Component.text(playerObjective.getBattleType().getPrefix(), NamedTextColor.GRAY)
                .append(Component.text(": ", NamedTextColor.DARK_GRAY))
                .append(Component.text(StringUtility.formatName(playerObjective.getObjectiveString()), NamedTextColor.BLUE))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text(isPlayerInTeam ? "Team-Points" : "Points", NamedTextColor.GRAY))
                .append(Component.text(": ", NamedTextColor.DARK_GRAY))
                .append(Component.text(points, NamedTextColor.BLUE));
    }
}
