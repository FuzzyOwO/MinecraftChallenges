package gg.itzkatze.mcc.util;

import gg.itzkatze.mcc.team.MccTeam;
import gg.itzkatze.mcc.player.MccPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Comparator;
import java.util.stream.Collectors;

public class ResultUtils {

    public static Component buildPlayerResultMessage(MccPlayer player) {
        int place = MccPlayer.getPlace(player);

        return Component.text("Results → ")
                .append(Component.text(player.getName()).color(NamedTextColor.GOLD))
                .append(Component.text(" | Place: " + place + " | Points: " + player.getPoints())
                        .color(NamedTextColor.YELLOW))
                .append(Component.text(" [Open Results]")
                        .color(NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.runCommand("/result player " + player.getName()))
                );
    }

    public static Component buildTeamResultMessage(MccTeam team) {
        int place = MccTeam.getPlace(team);
        String players = team.getPlayers().stream()
                .sorted(Comparator.comparingInt(MccPlayer::getPoints).reversed())
                .map(p -> p.getName() + " (" + p.getPoints() + ")")
                .collect(Collectors.joining(", "));

        return Component.text("Results → ")
                .append(Component.text("Team " + team.getId()).color(NamedTextColor.GOLD))
                .append(Component.text(" [" + players + "] ")).color(NamedTextColor.GRAY)
                .append(Component.text(" | Place: " + place + " | Points: " + team.getPoints())
                        .color(NamedTextColor.YELLOW))
                .append(Component.text(" [Open Results]")
                        .color(NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.runCommand("/result team " + team.getId())));
    }
}
