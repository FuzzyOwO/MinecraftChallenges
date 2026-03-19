package gg.itzkatze.mcc.forcebattle.commands;

import gg.itzkatze.mcc.MinecraftChallenges;
import gg.itzkatze.mcc.commands.framework.CallerType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import gg.itzkatze.mcc.commands.framework.MccCommand;
import gg.itzkatze.mcc.commands.framework.CommandCaller;
import gg.itzkatze.mcc.team.MccTeam;
import gg.itzkatze.mcc.forcebattle.gui.ResultGUI;
import gg.itzkatze.mcc.player.MccPlayer;
import gg.itzkatze.mcc.util.StringUtility;
import gg.itzkatze.mcc.util.TabCompletions;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ResultCommand extends MccCommand {

    public ResultCommand() {
        super(
                "result",
                List.of(),
                CallerType.PLAYER,
                "/result <team|player|playeranimated|teamanimated> <team id|player|place>",
                PermissionDefault.TRUE,
                "Command to display the results of a battle"
        );
    }

    @Override
    public void executeCommand(@NotNull CommandCaller caller, String[] args) {
        if (MinecraftChallenges.getTimer().isRunning()) {
            caller.sendMessage(Component.text("This command may only be used while timer is paused.").color(NamedTextColor.RED));
            return;
        }
        @NotNull Optional<MccPlayer> whoOpensOpt = MccPlayer.adapt(caller.getName());
        if (whoOpensOpt.isEmpty()) {
            return;
        }
        MccPlayer whoOpens = whoOpensOpt.get();
        if (args.length == 0) {
            new ResultGUI().open(whoOpens);
            return;
        }
        if (args.length < 2 && !args[0].equals("animated")) {
            sendUsage(caller);
            return;
        }
        switch (args[0]) {
            case "player" -> {
                @NotNull Optional<MccPlayer> targetOpt = MccPlayer.adapt(args[1]);
                targetOpt.ifPresentOrElse(
                        target -> new ResultGUI(target, false).open(whoOpens),
                        () -> caller.sendMessage(Component.text("Player couldn't be found.").color(NamedTextColor.RED))
                );
            }
            case "team" -> {
                int teamId;
                try {
                    teamId = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    caller.sendMessage(Component.text("Please specify a valid number.").color(NamedTextColor.RED));
                    return;
                }
                Optional<MccTeam> targetTeamOpt = MccTeam.ofId(teamId);
                targetTeamOpt.ifPresentOrElse(
                        team -> new ResultGUI(team, false).open(whoOpens),
                        () -> caller.sendMessage(Component.text("Team couldn't be found.").color(NamedTextColor.RED))
                );
            }
            case "playeranimated" -> {
                if (!caller.hasPermission(PermissionDefault.OP)) return;

                int place;
                try {
                    place = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    caller.sendMessage(Component.text("Please specify a valid number.").color(NamedTextColor.RED));
                    return;
                }
                MccPlayer mccPlayer = MccPlayer.getPlaces().get(place);
                for (MccPlayer player : MccPlayer.getOnlinePlayers()) {
                    new ResultGUI(mccPlayer, true).open(player);
                }
            }
            case "teamanimated" -> {
                if (!caller.hasPermission(PermissionDefault.OP)) return;

                int place;
                try {
                    place = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    caller.sendMessage(Component.text("Please specify a valid number.").color(NamedTextColor.RED));
                    return;
                }
                MccTeam team = MccTeam.getPlaces().get(place);
                if (team == null) return;
                for (MccPlayer player : MccPlayer.getOnlinePlayers()) {
                    new ResultGUI(team, true).open(player);
                }
            }
        }
    }

    @Override
    public List<String> tabComplete(CommandCaller caller, String @NotNull [] args) {
        if (args.length == 1) {
            return StringUtility.copyPartialMatches(args[0], List.of("player", "team", "playeranimated", "teamanimated"), new ArrayList<>());
        } else if (args.length == 2) {
            return switch (args[0]) {
                case "player" -> StringUtility.copyPartialMatches(args[1], TabCompletions.getPlayerNamesTabCompletions(), new ArrayList<>());
                case "team" -> StringUtility.copyPartialMatches(args[1], TabCompletions.getTeamIdTabCompletions(), new ArrayList<>());
                case "playeranimated" -> StringUtility.copyPartialMatches(args[1], TabCompletions.getPlayerPlaces(), new ArrayList<>());
                case "teamanimated" -> StringUtility.copyPartialMatches(args[1], TabCompletions.getTeamPlaces(), new ArrayList<>());
                default -> List.of();
            };
        }
        return List.of();
    }

}
