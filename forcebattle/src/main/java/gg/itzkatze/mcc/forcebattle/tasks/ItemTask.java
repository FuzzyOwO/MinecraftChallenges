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
import gg.itzkatze.mcc.util.StringUtility;
import gg.itzkatze.mcc.util.Toast;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

public class ItemTask implements ForceTask {

    @Override
    public void runTick() {
        if (ForceBattleType.FORCE_ITEM.getSettingState() == PlayerTeamBothState.OFF) return;
        if (!MinecraftChallenges.getTimer().isRunning()) return;

        for (MccPlayer player : MccPlayer.getOnlinePlayers()) {
            ForceBattlePlayer mccPlayer = (ForceBattlePlayer) player;
            if (!mccPlayer.isParticipating()) continue;

            checkPlayerObjective(mccPlayer);
            checkTeamObjective(mccPlayer);
        }
    }

    private void checkPlayerObjective(ForceBattlePlayer player) {
        ForceBattleObjective objective = player.getObjective();
        if (objective == null) return;
        if (objective.getBattleType() != ForceBattleType.FORCE_ITEM) return;

        Material material = objective.getObjectiveValue(Material.class);
        if (material != null && hasRequiredItem(player, material)) {
            completeObjective(player, objective, material);
            player.updateObjective(true, false);
        }
    }

    private void checkTeamObjective(ForceBattlePlayer player) {
        if (!player.isInTeam()) return;
        ForceBattleObjective teamObjective = (ForceBattleObjective) player.getTeam().getObjective();
        if (teamObjective == null) return;
        if (teamObjective.getBattleType() != ForceBattleType.FORCE_ITEM) return;

        Material material = teamObjective.getObjectiveValue(Material.class);
        if (material != null && hasRequiredItem(player, material)) {
            completeObjective(player, teamObjective, material);
            player.getTeam().updateObjective(player, true, false);
        }
    }

    private boolean hasRequiredItem(ForceBattlePlayer player, Material material) {
        return player.getInventory().contains(material);
    }

    private void completeObjective(ForceBattlePlayer player, ForceBattleObjective objective, Material material) {
        String formattedObjective = StringUtility.formatName(objective.getObjectiveString());
        Material toastIcon = material != null ? material : Material.BOOK;
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
