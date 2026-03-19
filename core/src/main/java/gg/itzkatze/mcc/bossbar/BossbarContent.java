package gg.itzkatze.mcc.bossbar;

import gg.itzkatze.mcc.player.MccPlayer;
import net.kyori.adventure.bossbar.BossBar;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface BossbarContent {

    @NotNull BossbarKind getKind();

    @Nullable BossBar getBossbar(@NotNull MccPlayer mccPlayer);

    default int getPriority() {
        return 0;
    }
}
