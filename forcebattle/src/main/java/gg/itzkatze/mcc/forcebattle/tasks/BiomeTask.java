package gg.itzkatze.mcc.forcebattle.tasks;

import gg.itzkatze.mcc.MinecraftChallenges;
import gg.itzkatze.mcc.forcebattle.ForceBattleObjective;
import gg.itzkatze.mcc.configuration.GlobalSetting;
import gg.itzkatze.mcc.configuration.SettingsManager;
import gg.itzkatze.mcc.configuration.states.PlayerTeamBothState;
import gg.itzkatze.mcc.forcebattle.ForceBattlePlayer;
import net.kyori.adventure.text.format.NamedTextColor;
import gg.itzkatze.mcc.data.BiomeSimplified;
import gg.itzkatze.mcc.player.MccPlayer;
import gg.itzkatze.mcc.forcebattle.ForceBattleType;
import gg.itzkatze.mcc.util.StringUtility;
import gg.itzkatze.mcc.util.Toast;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

public class BiomeTask implements ForceTask {

    @Override
    public void runTick() {
        if (ForceBattleType.FORCE_BIOME.getSettingState() == PlayerTeamBothState.OFF) return;
        if (!MinecraftChallenges.getTimer().isRunning()) return;

        for (MccPlayer mccPlayer : MccPlayer.getOnlinePlayers()) {
            ForceBattlePlayer fbPlayer = (ForceBattlePlayer) mccPlayer;
            Player player = mccPlayer.getPlayer();

            if (player == null) continue;
            if (!mccPlayer.isParticipating()) continue;

            Biome currentBiome = player.getWorld().getBiome(player.getLocation());
            checkPlayerObjective(fbPlayer, currentBiome);
            checkTeamObjective(fbPlayer, currentBiome);
        }
    }

    private void checkPlayerObjective(ForceBattlePlayer player, Biome currentBiome) {
        ForceBattleObjective objective = player.getObjective();
        if (objective == null) return;
        if (objective.getBattleType() != ForceBattleType.FORCE_BIOME) return;

        if (isBiomeMatch(objective.getObjectiveValue(), currentBiome)) {
            completeObjective(player, objective.getObjectiveString());
            player.updateObjective(true, false);
        }
    }

    private void checkTeamObjective(ForceBattlePlayer player, Biome currentBiome) {
        if (!player.isInTeam()) return;
        ForceBattleObjective teamObjective = (ForceBattleObjective) player.getTeam().getObjective();
        if (teamObjective == null) return;
        if (teamObjective.getBattleType() != ForceBattleType.FORCE_BIOME) return;

        if (isBiomeMatch(teamObjective.getObjectiveValue(), currentBiome)) {
            completeObjective(player, teamObjective.getObjectiveString());
            player.getTeam().updateObjective(player, true, false);
        }
    }

    private boolean isBiomeMatch(Object objectiveValue, Biome currentBiome) {
        if (objectiveValue instanceof BiomeSimplified simplified) {
            return simplified.getBiomes().contains(currentBiome);
        }

        if (objectiveValue instanceof Biome biome) {
            return biome == currentBiome;
        }

        return false;
    }

    private void completeObjective(ForceBattlePlayer player, String objective) {
        String formattedObjective = StringUtility.formatName(objective);
        Material toastIcon = Material.GRASS_BLOCK;
        MinecraftChallenges plugin = MinecraftChallenges.get();

        if (player.isInTeam() && SettingsManager.isEnabled(GlobalSetting.EXTRA_TEAM_OBJECTIVE)) {
            player.getTeam().getPlayers().stream()
                    .filter(teammate -> teammate != player)
                    .filter(teammate -> !teammate.isOffline())
                    .forEach(teammate -> {
                        teammate.sendMessage(Component.text(player.getName() + " has finished " + formattedObjective + "."));
                        Toast.display(teammate.getPlayer(), toastIcon, Component.text(formattedObjective).color(NamedTextColor.BLUE), Toast.Style.GOAL, plugin);
                    });
        }

        player.sendMessage(Component.text("You finished " + formattedObjective + "."));
        Toast.display(player.getPlayer(), toastIcon, Component.text(formattedObjective).color(NamedTextColor.BLUE), Toast.Style.GOAL, plugin);
    }
}
