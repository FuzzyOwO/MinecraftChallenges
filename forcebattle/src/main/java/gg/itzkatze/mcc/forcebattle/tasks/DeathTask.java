package gg.itzkatze.mcc.forcebattle.tasks;

import gg.itzkatze.mcc.MinecraftChallenges;
import gg.itzkatze.mcc.configuration.GlobalSetting;
import gg.itzkatze.mcc.configuration.SettingsManager;
import gg.itzkatze.mcc.configuration.states.PlayerTeamBothState;
import gg.itzkatze.mcc.data.DamageTypeSimplified;
import gg.itzkatze.mcc.data.MccDamageType;
import gg.itzkatze.mcc.forcebattle.ForceBattleObjective;
import gg.itzkatze.mcc.forcebattle.ForceBattlePlayer;
import gg.itzkatze.mcc.forcebattle.ForceBattleType;
import gg.itzkatze.mcc.player.MccPlayer;
import gg.itzkatze.mcc.util.StringUtility;
import gg.itzkatze.mcc.util.Toast;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;

public class DeathTask {

    public static void onDeath(PlayerDeathEvent event) {
        if (ForceBattleType.FORCE_DEATH.getSettingState() == PlayerTeamBothState.OFF) return;

        Player damager = event.getPlayer();

        if (!(MccPlayer.adapt(damager) instanceof ForceBattlePlayer mccPlayer)) return;
        if (!mccPlayer.isParticipating()) return;

        checkPlayerObjective(mccPlayer, event.getDamageSource().getDamageType(), event);
        checkTeamObjective(mccPlayer, event.getDamageSource().getDamageType(), event);
    }

    private static void checkPlayerObjective(ForceBattlePlayer player, DamageType damageSource, PlayerDeathEvent event) {
        ForceBattleObjective objective = player.getObjective();
        if (objective == null) return;
        if (objective.getBattleType() != ForceBattleType.FORCE_DEATH) return;

        if (isDamageSourceMatch(objective.getObjectiveValue(), damageSource)) {
            completeObjective(player, objective.getObjectiveString());
            returnItems(event);
            player.updateObjective(true, false);
        }
    }

    private static void checkTeamObjective(ForceBattlePlayer player, DamageType damageSource, PlayerDeathEvent event) {
        if (!player.isInTeam()) return;
        ForceBattleObjective teamObjective = (ForceBattleObjective) player.getTeam().getObjective();
        if (teamObjective == null) return;
        if (teamObjective.getBattleType() != ForceBattleType.FORCE_DEATH) return;

        if (isDamageSourceMatch(teamObjective.getObjectiveValue(), damageSource)) {
            completeObjective(player, teamObjective.getObjectiveString());
            returnItems(event);
            player.getTeam().updateObjective(player, true, false);
        }
    }

    private static boolean isDamageSourceMatch(Object objectiveValue, DamageType damageSource) {
        if (objectiveValue instanceof DamageTypeSimplified simplified) {
            for (MccDamageType cause : simplified.getDamageTypes()) {
                if (cause.getTypes().contains(damageSource)) return true;
            }
        }

        if (objectiveValue instanceof MccDamageType damageType) {
            return damageType.getTypes().contains(damageSource);
        }

        return false;
    }

    private static void completeObjective(ForceBattlePlayer player, String objective) {
        String formattedObjective = StringUtility.formatName(objective);
        Material toastIcon = Material.SKELETON_SKULL;
        MinecraftChallenges plugin = MinecraftChallenges.get();

        if (player.isInTeam() && SettingsManager.isEnabled(GlobalSetting.EXTRA_TEAM_OBJECTIVE)) {
            player.getTeam().getPlayers().stream()
                    .filter(teammate -> teammate != player)
                    .forEach(teammate -> {
                        teammate.sendMessage(Component.text(player.getName() + " has finished " + formattedObjective + "."));
                        Player teammatePlayer = teammate.getPlayer();
                        if (teammatePlayer != null) {
                            Toast.display(teammatePlayer, toastIcon, Component.text(formattedObjective).color(NamedTextColor.BLUE), Toast.Style.GOAL, plugin);
                        }
                    });
        }

        player.sendMessage(Component.text("You finished " + formattedObjective + "."));
        Player playerObj = player.getPlayer();
        if (playerObj != null) {
            Toast.display(playerObj, toastIcon, Component.text(formattedObjective).color(NamedTextColor.BLUE), Toast.Style.GOAL, plugin);
        }
    }

    private static void returnItems(PlayerDeathEvent event) {
        event.setKeepInventory(true);
        event.getDrops().clear();
    }
}
