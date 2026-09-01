package gg.itzkatze.mcc.forcebattle.tasks;

import gg.itzkatze.mcc.MinecraftChallenges;
import gg.itzkatze.mcc.forcebattle.ForceBattleObjective;
import gg.itzkatze.mcc.configuration.GlobalSetting;
import gg.itzkatze.mcc.configuration.SettingsManager;
import gg.itzkatze.mcc.configuration.states.PlayerTeamBothState;
import gg.itzkatze.mcc.forcebattle.ForceBattlePlayer;
import net.kyori.adventure.text.format.NamedTextColor;
import gg.itzkatze.mcc.player.MccPlayer;
import gg.itzkatze.mcc.forcebattle.ForceBattleType;
import gg.itzkatze.mcc.util.Toast;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class HeightTask implements ForceTask {

    @Override
    public void runTick() {
        if (ForceBattleType.FORCE_HEIGHT.getSettingState() == PlayerTeamBothState.OFF) return;
        if (!MinecraftChallenges.getTimer().isRunning()) return;

        for (MccPlayer player : MccPlayer.getOnlinePlayers()) {
            ForceBattlePlayer mccPlayer = (ForceBattlePlayer) player;
            if (mccPlayer.isExcluded()) continue;

            checkPlayerObjective(mccPlayer);
            checkTeamObjective(mccPlayer);
        }
    }

    private void checkPlayerObjective(ForceBattlePlayer player) {
        ForceBattleObjective objective = player.getObjective();
        if (objective == null) return;
        if (objective.getBattleType() != ForceBattleType.FORCE_HEIGHT) return;

        Integer height = objective.getObjectiveValue(Integer.class);
        if (height != null && isAtTargetHeight(player, height)) {
            completeObjective(player, objective.getObjectiveString(), height);
            player.updateObjective(true, false);
        }
    }

    private void checkTeamObjective(ForceBattlePlayer player) {
        if (!player.isInTeam()) return;
        ForceBattleObjective teamObjective = (ForceBattleObjective) player.getTeam().getObjective();
        if (teamObjective == null) return;
        if (teamObjective.getBattleType() != ForceBattleType.FORCE_HEIGHT) return;

        Integer height = teamObjective.getObjectiveValue(Integer.class);
        if (height != null && isAtTargetHeight(player, height)) {
            completeObjective(player, teamObjective.getObjectiveString(), height);
            player.getTeam().updateObjective(player, true, false);
        }
    }

    private boolean isAtTargetHeight(ForceBattlePlayer fbPlayer, int height) {
        Player player = fbPlayer.getPlayer();
        if (player == null) return false;

        return player.getLocation().getBlockY() == height;
    }

    private void completeObjective(ForceBattlePlayer player, String objective, int height) {
        String heightDisplay = String.valueOf(height);

        Material toastIcon = Material.SCAFFOLDING;
        MinecraftChallenges plugin = MinecraftChallenges.get();

        if (player.isInTeam() && SettingsManager.isEnabled(GlobalSetting.EXTRA_TEAM_OBJECTIVE)) {
            player.getTeam().getPlayers().stream()
                    .filter(teammate -> teammate != player)
                    .filter(teammate -> !teammate.isOffline())
                    .forEach(teammate -> {
                        teammate.sendMessage(Component.text(player.getName() + " has finished " + heightDisplay + "."));
                        Toast.display(teammate.getPlayer(), toastIcon, Component.text(heightDisplay).color(NamedTextColor.BLUE), Toast.Style.GOAL, plugin);
                    });
        }

        player.sendMessage(Component.text("You finished " + heightDisplay + "."));
        Toast.display(player.getPlayer(), toastIcon, Component.text(heightDisplay).color(NamedTextColor.BLUE), Toast.Style.GOAL, plugin);
    }
}
