package gg.itzkatze.mcc.forcebattle.bossbar;

import gg.itzkatze.mcc.bossbar.BossbarContent;
import gg.itzkatze.mcc.bossbar.BossbarKind;
import gg.itzkatze.mcc.forcebattle.ForceBattleObjective;
import gg.itzkatze.mcc.configuration.GlobalSetting;
import gg.itzkatze.mcc.configuration.SettingsManager;
import gg.itzkatze.mcc.team.MccTeam;
import gg.itzkatze.mcc.player.MccPlayer;
import gg.itzkatze.mcc.util.StringUtility;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ForceBattleTeamObjectiveBossbar implements BossbarContent {

    @Override
    @NotNull public BossbarKind getKind() {
        return BossbarKind.RUNNING;
    }

    @Override
    @Nullable public BossBar getBossbar(@NotNull MccPlayer player) {
        if (!player.isInTeam()) return null;

        MccTeam team = player.getTeam();
        ForceBattleObjective objective = (ForceBattleObjective) team.getObjective();
        if (objective == null) return null;

        return BossBar.bossBar(
                getTitle(team),
                1,
                BossBar.Color.PURPLE,
                BossBar.Overlay.PROGRESS
        );
    }

    private @NotNull Component getTitle(MccTeam team) {
        ForceBattleObjective teamObjective = (ForceBattleObjective) team.getObjective();
        if (teamObjective == null) return Component.empty();

        if (SettingsManager.isEnabled(GlobalSetting.HIDE_POINTS)) {
            return Component.text(teamObjective.getBattleType().getPrefix(), NamedTextColor.GRAY)
                    .append(Component.text(": ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(StringUtility.formatName(teamObjective.getObjectiveString()), NamedTextColor.BLUE));
        }

        return Component.text(teamObjective.getBattleType().getPrefix(), NamedTextColor.GRAY)
                .append(Component.text(": ", NamedTextColor.DARK_GRAY))
                .append(Component.text(StringUtility.formatName(teamObjective.getObjectiveString()), NamedTextColor.BLUE))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text("Team-Points", NamedTextColor.GRAY))
                .append(Component.text(": ", NamedTextColor.DARK_GRAY))
                .append(Component.text(team.getPoints(), NamedTextColor.BLUE));
    }
}
