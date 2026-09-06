package gg.itzkatze.mcc.forcebattle.tasks;

import gg.itzkatze.mcc.MinecraftChallenges;
import gg.itzkatze.mcc.configuration.GlobalSetting;
import gg.itzkatze.mcc.configuration.SettingsManager;
import gg.itzkatze.mcc.configuration.states.PlayerTeamBothState;
import gg.itzkatze.mcc.data.MobsSimplified;
import gg.itzkatze.mcc.forcebattle.ForceBattleObjective;
import gg.itzkatze.mcc.forcebattle.ForceBattlePlayer;
import gg.itzkatze.mcc.forcebattle.ForceBattleType;
import gg.itzkatze.mcc.player.MccPlayer;
import gg.itzkatze.mcc.util.StringUtility;
import gg.itzkatze.mcc.util.Toast;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class MobsTask {

    public static void onDamage(EntityDamageByEntityEvent event) {
        if (ForceBattleType.FORCE_MOB.getSettingState() == PlayerTeamBothState.OFF) return;

        Player damager;

        if (!(event.getEntity() instanceof LivingEntity target)) return;
        if (event.getDamager() instanceof Fireball fireball) {
            if (fireball.getShooter() instanceof Player player) {
                damager = player;
            } else return;
        } else if (event.getDamager() instanceof Player player) {
            damager = player;
        } else return;

        if (!(MccPlayer.adapt(damager) instanceof ForceBattlePlayer mccPlayer)) return;
        if (mccPlayer.isExcluded()) return;

        if (target.getHealth() - event.getDamage() > 0) return;

        checkPlayerObjective(mccPlayer, target.getType());
        checkTeamObjective(mccPlayer, target.getType());
    }

    private static void checkPlayerObjective(ForceBattlePlayer player, EntityType target) {
        ForceBattleObjective objective = player.getObjective();
        if (objective == null) return;
        if (objective.getBattleType() != ForceBattleType.FORCE_MOB) return;

        if (isMobMatch(objective.getObjectiveValue(), target)) {
            completeObjective(player, objective.getObjectiveString());
            player.updateObjective(true, false);
        }
    }

    private static void checkTeamObjective(ForceBattlePlayer player, EntityType target) {
        if (!player.isInTeam()) return;
        ForceBattleObjective teamObjective = (ForceBattleObjective) player.getTeam().getObjective();
        if (teamObjective == null) return;
        if (teamObjective.getBattleType() != ForceBattleType.FORCE_MOB) return;

        if (isMobMatch(teamObjective.getObjectiveValue(), target)) {
            completeObjective(player, teamObjective.getObjectiveString());
            player.getTeam().updateObjective(player, true, false);
        }
    }

    private static boolean isMobMatch(Object objectiveValue, EntityType target) {
        if (objectiveValue instanceof MobsSimplified simplified) {
            return simplified.getMobs().contains(target);
        }

        if (objectiveValue instanceof EntityType entityType) {
            return entityType == target;
        }

        return false;
    }

    private static void completeObjective(ForceBattlePlayer player, String objective) {
        String formattedObjective = StringUtility.formatName(objective);
        Material toastIcon = Material.DIAMOND_SWORD;
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
}
