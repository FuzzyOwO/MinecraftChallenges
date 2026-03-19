package gg.itzkatze.mcc.forcebattle.commands;

import gg.itzkatze.mcc.MinecraftChallenges;
import gg.itzkatze.mcc.commands.framework.CallerType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import gg.itzkatze.mcc.commands.framework.MccCommand;
import gg.itzkatze.mcc.commands.framework.CommandCaller;
import gg.itzkatze.mcc.team.MccTeam;
import gg.itzkatze.mcc.player.MccPlayer;
import gg.itzkatze.mcc.util.StringUtility;
import org.bukkit.permissions.PermissionDefault;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomTeamsCommand extends MccCommand {

    public RandomTeamsCommand() {
        super(
                "randomteams",
                List.of(),
                CallerType.PLAYER,
                "/randomteams <teamsize> <mode>",
                PermissionDefault.OP,
                "Command to create random teams."
        );
    }

    @Override
    protected void executeCommand(final CommandCaller caller, final String[] args) {
        if (MinecraftChallenges.getTimer().isRunning()) {
            caller.sendMessage(Component.text("The game has already started.").color(NamedTextColor.RED));
            return;
        }

        if (args.length == 0 || args[0].isEmpty()) {
            sendUsage(caller);
            return;
        }

        int size;
        try {
            size = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            caller.sendMessage(Component.text("Please specify a valid number.").color(NamedTextColor.RED));
            return;
        }

        List<MccPlayer> players = new ArrayList<>(MccPlayer.getOnlinePlayers());
        int playerAmount = players.size();

        if (playerAmount % size != 0) {
            caller.sendMessage(Component.text("No equal teams can be created.").color(NamedTextColor.RED));
            return;
        }

        int amountOfTeams = playerAmount / size;
        Random random = new Random();

        if (args.length > 1 && args[1].equalsIgnoreCase("fillExistingTeams")) {
            List<MccTeam> existingTeams = new ArrayList<>(MccTeam.teams);

            for (MccTeam team : existingTeams) {
                for (MccPlayer p : team.getPlayers()) {
                    players.remove(p);
                }
            }

            for (MccTeam team : existingTeams) {
                while (team.getPlayers().size() > size) {
                    MccPlayer removed = team.getPlayers().get(random.nextInt(team.getPlayers().size()));
                    team.removePlayer(removed);
                    players.add(removed);
                }
            }

            for (MccTeam team : existingTeams) {
                while (team.getPlayers().size() < size && !players.isEmpty()) {
                    MccPlayer randomPlayer = players.remove(random.nextInt(players.size()));
                    team.addPlayer(randomPlayer);
                }
            }

            while (!players.isEmpty()) {
                MccPlayer first = players.remove(random.nextInt(players.size()));
                MccTeam team = MccTeam.create(List.of(first));

                while (team.getPlayers().size() < size && !players.isEmpty()) {
                    MccPlayer next = players.remove(random.nextInt(players.size()));
                    team.addPlayer(next);
                }
            }

        } else {
            MccTeam.deleteAll();

            for (int i = 0; i < amountOfTeams; i++) {
                MccPlayer first = players.remove(random.nextInt(players.size()));
                MccTeam team = MccTeam.create(List.of(first));

                while (team.getPlayers().size() < size && !players.isEmpty()) {
                    MccPlayer next = players.remove(random.nextInt(players.size()));
                    team.addPlayer(next);
                }
            }
        }

        caller.sendMessage(Component.text("Successfully created " + amountOfTeams + " Teams.").color(NamedTextColor.GREEN));
    }

    @Override
    protected List<String> tabComplete(final CommandCaller caller, final String[] args) {
        if (args.length == 2) {
            return StringUtility.copyPartialMatches(args[1], List.of("fillExistingTeams"), new ArrayList<>());
        }
        return List.of();
    }
}
