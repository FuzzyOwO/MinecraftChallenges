package gg.itzkatze.katze.mobarmy;

import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vex;

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;

final class MobArmyTargeting {
    private MobArmyTargeting() {}

    static boolean canFight(MobArmyMatch.Side side, Player player, World world) {
        return !side.finished && !side.preparing && side.members.containsKey(player.getUniqueId())
                && !side.roofTicks.containsKey(player.getUniqueId()) && !side.pendingBaseline.contains(player.getUniqueId())
                && player.isOnline() && !player.isDead() && player.getGameMode() == GameMode.SURVIVAL && Objects.equals(world, player.getWorld());
    }

    static void update(Mob mob, MobArmyMatch.Side side, Collection<Player> players, boolean refreshVex) {
        LivingEntity current = mob.getTarget();
        boolean invalid = current != null && (!(current instanceof Player player) || !canFight(side, player, mob.getWorld()));
        if (!(invalid || mob instanceof IronGolem || mob instanceof Vex && (current == null || refreshVex))) return;
        Player nearest = players.stream().filter(player -> canFight(side, player, mob.getWorld()))
                .min(Comparator.comparingDouble(player -> player.getLocation().distanceSquared(mob.getLocation()))).orElse(null);
        if (mob instanceof IronGolem golem) golem.setPlayerCreated(false);
        if (current != nearest || mob instanceof Vex && refreshVex) mob.setTarget(nearest);
    }
}
