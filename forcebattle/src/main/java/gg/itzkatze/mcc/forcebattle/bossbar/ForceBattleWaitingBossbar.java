package gg.itzkatze.mcc.forcebattle.bossbar;

import gg.itzkatze.mcc.bossbar.BossbarContent;
import gg.itzkatze.mcc.bossbar.BossbarKind;
import gg.itzkatze.mcc.player.MccPlayer;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ForceBattleWaitingBossbar implements BossbarContent {

    @Override
    @NotNull public BossbarKind getKind() {
        return BossbarKind.NOT_RUNNING;
    }

    @Override
    @Nullable public BossBar getBossbar(@NotNull MccPlayer player) {
        return BossBar.bossBar(
                Component.text("Waiting").color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE),
                1,
                BossBar.Color.YELLOW,
                BossBar.Overlay.PROGRESS
        );
    }
}
