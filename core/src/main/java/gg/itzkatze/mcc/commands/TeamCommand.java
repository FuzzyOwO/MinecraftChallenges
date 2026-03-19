package gg.itzkatze.mcc.commands;

import gg.itzkatze.mcc.MinecraftChallenges;
import gg.itzkatze.mcc.commands.framework.CallerType;
import gg.itzkatze.mcc.team.MccTeam;
import gg.itzkatze.mcc.commands.framework.MccCommand;
import gg.itzkatze.mcc.commands.framework.CommandCaller;
import gg.itzkatze.mcc.player.MccPlayer;
import gg.itzkatze.mcc.util.StringUtility;
import gg.itzkatze.mcc.util.TabCompletions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TeamCommand extends MccCommand {

    public TeamCommand() {
        super(
                "team",
                List.of(),
                CallerType.PLAYER,
                "/team <create|join|leave|list|accept|reject> <id>",
                PermissionDefault.TRUE,
                "Command to manage teams: create, join, etc."
        );
    }

    @Override
    public void executeCommand(CommandCaller caller, String[] args) {
        if (args.length < 1) {
            sendUsage(caller);
            return;
        }

        @NotNull Optional<MccPlayer> battlePlayerOpt = MccPlayer.adapt(caller.getName());
        if (battlePlayerOpt.isEmpty()) {
            return;
        }
        MccPlayer mccPlayer = battlePlayerOpt.get();
        switch (args[0]) {
            case "create" -> {
                if (MinecraftChallenges.getTimer().isRunning()) {
                    caller.sendMessage(Component.text("The game has already started.").color(NamedTextColor.RED));
                    return;
                }
                mccPlayer.leaveTeam();
                MccTeam team = MccTeam.create(List.of(mccPlayer));
                caller.sendMessage(Component.text("New team has been created. Team ID: " + team.getId()));
            }
            case "join" -> {
                if (args.length < 2) {
                    caller.sendMessage(Component.text("Please specify a valid team ID.").color(NamedTextColor.RED));
                    return;
                }
                if (MinecraftChallenges.getTimer().isRunning()) {
                    caller.sendMessage(Component.text("The game has already started.").color(NamedTextColor.RED));
                    return;
                }

                int teamId;
                try {
                    teamId = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    caller.sendMessage(Component.text("Please specify a valid team ID.").color(NamedTextColor.RED));
                    return;
                }

                Optional<MccTeam> teamOpt = MccTeam.ofId(teamId);
                teamOpt.ifPresentOrElse(
                        team -> team.sendJoinRequest(mccPlayer),
                        () -> caller.sendMessage(Component.text("Please specify a valid team ID.").color(NamedTextColor.RED))
                );
            }
            case "leave" -> {
                if (MinecraftChallenges.getTimer().isRunning()) {
                    caller.sendMessage(Component.text("The game has already started.").color(NamedTextColor.RED));
                    return;
                }
                mccPlayer.leaveTeam();
            }
            case "accept" -> {
                MccTeam team = mccPlayer.getTeam();
                if (team == null) {
                    caller.sendMessage(Component.text("You are not currently in a team.").color(NamedTextColor.GRAY));
                    return;
                }
                if (team.getLastJoinRequest() != null) {
                    team.finishLastJoinRequest(true);
                }
            }
            case "reject" -> {
                if (MinecraftChallenges.getTimer().isRunning()) {
                    caller.sendMessage(Component.text("The game has already started.").color(NamedTextColor.RED));
                    return;
                }
                MccTeam team = mccPlayer.getTeam();
                if (team == null) {
                    caller.sendMessage(Component.text("You are not currently in a team.").color(NamedTextColor.GRAY));
                    return;
                }
                if (team.getLastJoinRequest() != null) {
                    team.finishLastJoinRequest(false);
                }
            }
            case "list" -> {
                Component teamList = Component.text("Teams:", NamedTextColor.BLUE).decorate(TextDecoration.BOLD);
                for (MccTeam team : MccTeam.teams) {
                    teamList = teamList.append(Component.text("\n"))
                            .append(Component.text(team.getId() + ":", NamedTextColor.YELLOW));
                    List<MccPlayer> teamPlayers = team.getPlayers();
                    for (int i = 0; i < teamPlayers.size(); i++) {
                        MccPlayer teamPlayer = teamPlayers.get(i);
                        if (teamPlayer == null || teamPlayer.isOffline()) {
                            continue;
                        }
                        teamList = teamList.append(Component.text(" " + teamPlayer.getName(), NamedTextColor.BLUE));
                        if (i < teamPlayers.size() - 1) {
                            teamList = teamList.append(Component.text(",", NamedTextColor.BLUE));
                        }
                    }
                }

                caller.sendMessage(teamList);
            }
        }
    }

    @Override
    public List<String> tabComplete(CommandCaller caller, String @NotNull [] args) {
        if (args.length == 1) {
            return StringUtility.copyPartialMatches(args[0], List.of("create", "list", "join", "leave", "accept", "reject"), new ArrayList<>());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("join")) {
            return StringUtility.copyPartialMatches(args[1], TabCompletions.getTeamIdTabCompletions(), new ArrayList<>());
        }
        return List.of();
    }

}
