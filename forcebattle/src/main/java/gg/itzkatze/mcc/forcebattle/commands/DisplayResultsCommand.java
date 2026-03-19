package gg.itzkatze.mcc.forcebattle.commands;

import gg.itzkatze.mcc.MinecraftChallenges;
import gg.itzkatze.mcc.commands.framework.CallerType;
import gg.itzkatze.mcc.commands.framework.MccCommand;
import gg.itzkatze.mcc.commands.framework.CommandCaller;
import gg.itzkatze.mcc.team.MccTeam;
import gg.itzkatze.mcc.player.MccPlayer;
import gg.itzkatze.mcc.util.StringUtility;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DisplayResultsCommand extends MccCommand {

    public DisplayResultsCommand() {
        super(
                "displayresults",
                List.of(),
                CallerType.NONE,
                "/displayresults <player/team>",
                PermissionDefault.OP,
                "Command to display the results of the battle"
        );
    }

    @Override
    protected void executeCommand(CommandCaller caller, String[] args) {
        if (!MinecraftChallenges.getTimer().hasHitZero()) {
            caller.sendMessage(Component.text("The challenge is not yet over.").color(NamedTextColor.RED));
            return;
        }

        if (args.length == 0) {
            sendUsage(caller);
            return;
        }

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        final long delay = 750;
        final long[] counter = {0};

        switch (args[0].toLowerCase()) {
            case "player" -> {
                List<MccPlayer> sortedPlayers = new ArrayList<>(MccPlayer.PLAYERS);
                sortedPlayers.sort(Comparator.comparingInt(MccPlayer::getPoints).reversed());

                for (MccPlayer player : sortedPlayers) {
                    int place = MccPlayer.getPlace(player);
                    int points = player.getPoints();

                    scheduler.schedule(() -> MinecraftChallenges.broadcast(
                            Component.text(place + ". ", NamedTextColor.GOLD)
                                    .append(Component.text(player.getName(), NamedTextColor.BLUE))
                                    .append(Component.text(": ", NamedTextColor.DARK_GRAY))
                                    .append(Component.text(points, NamedTextColor.GOLD))
                    ), counter[0] * delay, TimeUnit.MILLISECONDS);
                    counter[0]++;
                }
            }

            case "team" -> {
                List<MccTeam> sortedTeams = new ArrayList<>(MccTeam.teams);
                sortedTeams.sort(Comparator.comparingInt(MccTeam::getPoints).reversed());

                for (MccTeam team : sortedTeams) {
                    int place = MccTeam.getPlace(team);
                    int points = team.getPoints();

                    String players = team.getPlayers().stream()
                            .sorted(Comparator.comparingInt(MccPlayer::getPoints).reversed())
                            .map(p -> p.getName() + " (" + p.getPoints() + ")")
                            .collect(Collectors.joining(", "));

                    scheduler.schedule(() -> MinecraftChallenges.broadcast(
                            Component.text(place + ". ", NamedTextColor.GOLD)
                                    .append(Component.text(String.valueOf(team.getId()), NamedTextColor.BLUE))
                                    .append(Component.text(" (", NamedTextColor.BLUE))
                                    .append(Component.text(players, NamedTextColor.GRAY))
                                    .append(Component.text(")", NamedTextColor.BLUE))
                                    .append(Component.text(": ", NamedTextColor.DARK_GRAY))
                                    .append(Component.text(points, NamedTextColor.GOLD))
                    ), counter[0] * delay, TimeUnit.MILLISECONDS);
                    counter[0]++;
                }
            }

            default -> sendUsage(caller);
        }

        scheduler.shutdown();
    }

    @Override
    public List<String> tabComplete(CommandCaller caller, String @NotNull [] args) {
        if (args.length == 1) {
            return StringUtility.copyPartialMatches(args[0], List.of("player", "team"), new ArrayList<>());
        }
        return List.of();
    }
}
