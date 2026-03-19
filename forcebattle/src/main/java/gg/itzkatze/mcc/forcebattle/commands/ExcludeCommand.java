package gg.itzkatze.mcc.forcebattle.commands;

import gg.itzkatze.mcc.commands.framework.CallerType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

public class ExcludeCommand extends MccCommand {

    public ExcludeCommand() {
        super(
                "exclude",
                List.of(),
                CallerType.NONE,
                "/exclude <player>",
                PermissionDefault.OP,
                "Command to exclude a player from the battle"
        );
    }

    @Override
    public void executeCommand(CommandCaller caller, String[] args) {
        if (args.length < 1) {
            if (!caller.callerType().equals(CallerType.PLAYER)) {
                caller.sendMessage(Component.text("Only players may use this command.").color(NamedTextColor.RED));
                return;
            }
            @NotNull Optional<MccPlayer> battlePlayerOpt = MccPlayer.adapt(caller.getName());
            battlePlayerOpt.ifPresent(battlePlayer -> battlePlayer.setExcluded(!battlePlayer.isExcluded()));
            return;
        }
        @NotNull Optional<MccPlayer> toExcludeOpt = MccPlayer.adapt(args[0]);
        toExcludeOpt.ifPresent(toExclude -> toExclude.setExcluded(!toExclude.isExcluded()));
    }

    @Override
    public List<String> tabComplete(CommandCaller caller, String @NotNull [] args) {
        if (args.length == 1) {
            return StringUtility.copyPartialMatches(args[0], TabCompletions.getPlayerNamesTabCompletions(), new ArrayList<>());
        }
        return List.of();
    }

}
