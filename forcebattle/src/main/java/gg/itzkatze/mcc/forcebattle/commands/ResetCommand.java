package gg.itzkatze.mcc.forcebattle.commands;

import gg.itzkatze.mcc.MinecraftChallenges;
import gg.itzkatze.mcc.commands.framework.CallerType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import gg.itzkatze.mcc.forcebattle.ForceBattlePlayer;
import gg.itzkatze.mcc.commands.framework.MccCommand;
import gg.itzkatze.mcc.commands.framework.CommandCaller;
import gg.itzkatze.mcc.player.MccPlayer;
import gg.itzkatze.mcc.util.StringUtility;
import gg.itzkatze.mcc.util.TabCompletions;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ResetCommand extends MccCommand {

    public ResetCommand() {
        super(
                "reset",
                List.of(),
                CallerType.NONE,
                "/reset <player>",
                PermissionDefault.OP,
                "Command to reset a player's state"
        );
    }

    @Override
    public void executeCommand(CommandCaller caller, String @NotNull [] args) {
        if (args.length < 1) {
            MinecraftChallenges.getTimer().setRunning(false);
            MinecraftChallenges.getTimer().setTime(MinecraftChallenges.getTimer().getStartTime());
            MccPlayer.PLAYERS.stream()
                    .filter(ForceBattlePlayer.class::isInstance)
                    .map(ForceBattlePlayer.class::cast)
                    .forEach(fp -> fp.reset(true));
            return;
        }
        @NotNull Optional<MccPlayer> toResetOpt = MccPlayer.adapt(args[0]);
        toResetOpt.ifPresentOrElse(
                toReset -> {
                    if (toReset instanceof ForceBattlePlayer fp) fp.reset(true);
                },
                () -> caller.sendMessage(Component.text("Player couldn't be found.").color(NamedTextColor.RED))
        );
    }

    @Override
    public List<String> tabComplete(CommandCaller caller, String @NotNull [] args) {
        if (args.length == 1) {
            return StringUtility.copyPartialMatches(args[0], TabCompletions.getPlayerNamesTabCompletions(), new ArrayList<>());
        }
        return List.of();
    }

}
