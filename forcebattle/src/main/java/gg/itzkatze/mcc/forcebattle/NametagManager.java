package gg.itzkatze.mcc.forcebattle;

import gg.itzkatze.mcc.MinecraftChallenges;
import gg.itzkatze.mcc.configuration.GlobalSetting;
import gg.itzkatze.mcc.configuration.SettingsManager;
import gg.itzkatze.mcc.data.MccAdvancement;
import gg.itzkatze.mcc.team.MccTeam;
import gg.itzkatze.mcc.player.MccPlayer;
import gg.itzkatze.mcc.scoreboard.ScoreboardManager;
import gg.itzkatze.mcc.util.StringUtility;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NametagManager {

    private static final Map<UUID, Map<UUID, Component>> lastSuffixes = new ConcurrentHashMap<>();

    public static void runTask() {
        Bukkit.getScheduler().runTaskTimer(MinecraftChallenges.get(), () ->
                MccPlayer.PLAYERS.forEach(NametagManager::updateNametag), 0, 3);
    }

    private static void updateNametag(@NotNull MccPlayer mccPlayer) {
        if (!(mccPlayer instanceof ForceBattlePlayer)) return;
        Player player = mccPlayer.getPlayer();
        if (player == null) return;

        Scoreboard scoreboard = ScoreboardManager.getOrCreateScoreboard(mccPlayer);
        Map<UUID, Component> cache = lastSuffixes.computeIfAbsent(mccPlayer.getUuid(), u -> new ConcurrentHashMap<>());

        for (MccPlayer targetPlayer : MccPlayer.PLAYERS) {
            Player target = targetPlayer.getPlayer();
            if (target == null) continue;

            Team team = scoreboard.getTeam(targetPlayer.getUuid().toString());
            if (team == null) {
                team = scoreboard.registerNewTeam(targetPlayer.getUuid().toString());
            }
            if (!team.hasEntry(targetPlayer.getName())) {
                team.addEntry(targetPlayer.getName());
            }

            Component newSuffix = buildSuffix((ForceBattlePlayer) targetPlayer);
            Component oldSuffix = team.suffix();

            if (!Objects.equals(newSuffix, oldSuffix)) {
                team.suffix(newSuffix);
                cache.put(targetPlayer.getUuid(), newSuffix);
            }
        }

        player.setScoreboard(scoreboard);
    }

    private static Component buildSuffix(@NotNull ForceBattlePlayer player) {
        Component suffix = Component.text(" ");

        if (player.isExcluded()) {
            suffix = suffix.append(Component.text("Excluded", NamedTextColor.GRAY)
                    .decorate(TextDecoration.ITALIC));
        } else if (!MinecraftChallenges.getTimer().isRunning()) {
            suffix = suffix.append(Component.text("Waiting", NamedTextColor.GRAY)
                    .decorate(TextDecoration.ITALIC));
        }

        ForceBattleObjective objective = player.getObjective();
        if (objective == null && player.isInTeam()) {
            objective = (ForceBattleObjective) player.getTeam().getObjective();
        }

        if (objective != null) {
            String objectiveString;
            MccAdvancement advancement = objective.getObjectiveValue(MccAdvancement.class);
            if (advancement != null) {
                objectiveString = advancement.name;
            } else {
                objectiveString = StringUtility.formatName(objective.getObjectiveString());
            }

            boolean hideObjectives = SettingsManager.isEnabled(GlobalSetting.HIDE_OBJECTIVES);
            boolean hidePoints = SettingsManager.isEnabled(GlobalSetting.HIDE_POINTS);

            if (!hideObjectives) {
                suffix = suffix.append(Component.text(" "))
                        .append(Component.text(objective.getBattleType().getPrefix(), NamedTextColor.GRAY))
                        .append(Component.text(" » ", NamedTextColor.DARK_GRAY))
                        .append(Component.text(objectiveString, NamedTextColor.BLUE));
            }

            if (!hideObjectives && !hidePoints) {
                suffix = suffix.append(Component.text(" |", NamedTextColor.DARK_GRAY));
            }

            if (!hidePoints) {
                suffix = suffix.append(Component.text(" "))
                        .append(Component.text("Points", NamedTextColor.GRAY))
                        .append(Component.text(" » ", NamedTextColor.DARK_GRAY))
                        .append(Component.text(player.getPoints(), NamedTextColor.BLUE));
            }
        }

        MccTeam teamData = player.getTeam();
        if (teamData != null) {
            suffix = suffix.append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                    .append(Component.text("Team", NamedTextColor.GRAY))
                    .append(Component.text(" » ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(teamData.getId(), NamedTextColor.BLUE));
        }

        return suffix.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }
}
