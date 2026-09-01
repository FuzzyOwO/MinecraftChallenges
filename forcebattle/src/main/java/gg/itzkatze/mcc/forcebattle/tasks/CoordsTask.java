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
import org.bukkit.Location;
import org.bukkit.Material;

public class CoordsTask implements ForceTask {

    private static final double COMPLETION_RADIUS = 2.0;

    @Override
    public void runTick() {
        if (ForceBattleType.FORCE_COORDS.getSettingState() == PlayerTeamBothState.OFF) return;
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
        if (objective.getBattleType() != ForceBattleType.FORCE_COORDS) return;

        Location target = objective.getObjectiveValue(Location.class);
        if (target != null && isAtCoordinates(player, target)) {
            completeObjective(player, target);
            player.updateObjective(true, false);
        }
    }

    private void checkTeamObjective(ForceBattlePlayer player) {
        if (!player.isInTeam()) return;
        ForceBattleObjective teamObjective = (ForceBattleObjective) player.getTeam().getObjective();
        if (teamObjective == null) return;
        if (teamObjective.getBattleType() != ForceBattleType.FORCE_COORDS) return;

        Location target = teamObjective.getObjectiveValue(Location.class);
        if (target != null && isAtCoordinates(player, target)) {
            completeObjective(player, target);
            player.getTeam().updateObjective(player, true, false);
        }
    }

    private boolean isAtCoordinates(ForceBattlePlayer player, Location target) {
        Location playerLoc = player.getPlayer().getLocation();
        Location targetLoc = new Location(player.getWorld(), target.getX(), playerLoc.getY(), target.getZ());
        double distance = playerLoc.distance(targetLoc);

        return distance <= COMPLETION_RADIUS;
    }

    private void completeObjective(ForceBattlePlayer player, Location target) {
        String coordsDisplay = String.format("%d, %d", target.getBlockX(), target.getBlockZ());

        Material toastIcon = Material.COMPASS;
        MinecraftChallenges plugin = MinecraftChallenges.get();

        if (player.isInTeam() && SettingsManager.isEnabled(GlobalSetting.EXTRA_TEAM_OBJECTIVE)) {
            player.getTeam().getPlayers().stream()
                    .filter(teammate -> teammate != player)
                    .filter(teammate -> !teammate.isOffline())
                    .forEach(teammate -> {
                        teammate.sendMessage(Component.text(player.getName() + " has finished " + coordsDisplay + "."));
                        Toast.display(teammate.getPlayer(), toastIcon, Component.text(coordsDisplay).color(NamedTextColor.BLUE), Toast.Style.GOAL, plugin);
                    });
        }

        player.sendMessage(Component.text("You finished " + coordsDisplay + "."));
        Toast.display(player.getPlayer(), toastIcon, Component.text(coordsDisplay).color(NamedTextColor.BLUE), Toast.Style.GOAL, plugin);
    }
}
