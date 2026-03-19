package gg.itzkatze.mcc.forcebattle.commands;

import gg.itzkatze.mcc.commands.framework.CallerType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import gg.itzkatze.mcc.commands.framework.MccCommand;
import gg.itzkatze.mcc.commands.framework.CommandCaller;
import gg.itzkatze.mcc.team.MccTeam;
import gg.itzkatze.mcc.player.MccPlayer;
import gg.itzkatze.mcc.util.StringUtility;
import gg.itzkatze.mcc.util.TabCompletions;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SkipCommand extends MccCommand {

    public SkipCommand() {
        super(
                "skip",
                List.of(),
                CallerType.NONE,
                "/skip <player/team> <player/team>",
                PermissionDefault.OP,
                "Command to skip a player's objective - meant for admins if an item is unobtainable"
        );
    }

    @Override
    public void executeCommand(CommandCaller caller, String[] args) {
        if (args.length > 0) {
            switch (args[0]) {
                case "player" -> {
                    @NotNull Optional<MccPlayer> battlePlayerOpt = MccPlayer.adapt(args[1]);
                    battlePlayerOpt.ifPresentOrElse(
                            battlePlayer -> {
                                if (battlePlayer.isOffline()) {
                                    return;
                                }
                                battlePlayer.updateObjective(false, false);
                                battlePlayer.sendMessage(Component.text("Your objective has been skipped by an admin.").color(NamedTextColor.GREEN));
                                caller.sendMessage(Component.text("Successfully skipped the objective of " + battlePlayer.getName() + ".").color(NamedTextColor.GREEN));
                            }, () -> caller.sendMessage(Component.text("Player couldn't be found.").color(NamedTextColor.RED))
                    );
                }
                case "team" -> {
                    try {
                        @NotNull Optional<MccTeam> teamOpt = MccTeam.ofId(Integer.parseInt(args[1]));
                        teamOpt.ifPresentOrElse(
                                team -> {
                                    team.updateObjective(null, false, false);
                                    team.getPlayers().forEach(player -> player.sendMessage(Component.text("Your objective has been skipped by an admin.").color(NamedTextColor.GREEN)));
                                    caller.sendMessage(Component.text("Successfully skipped the objective of " + team.getId() + ".").color(NamedTextColor.GREEN));
                                }, () -> caller.sendMessage(Component.text("Team couldn't be found.").color(NamedTextColor.RED))
                        );
                    } catch (NumberFormatException e) {
                        caller.sendMessage(Component.text("Please specify a valid number.").color(NamedTextColor.RED));
                    }
                }
            }
        } else {
            sendUsage(caller);
        }
    }

    @Override
    public List<String> tabComplete(CommandCaller caller, String @NotNull [] args) {
        if (args.length == 1) {
            return StringUtility.copyPartialMatches(args[0], List.of("player", "team"), new ArrayList<>());
        } else if (args.length == 2) {
            return switch (args[0]) {
                case "player" -> StringUtility.copyPartialMatches(args[1], TabCompletions.getPlayerNamesTabCompletions(), new ArrayList<>());
                case "team" -> StringUtility.copyPartialMatches(args[1], TabCompletions.getTeamIdTabCompletions(), new ArrayList<>());
                default -> List.of();
            };
        }
        return List.of();
    }

}
