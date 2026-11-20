package gg.itzkatze.katze.mobarmy;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vex;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MobArmyTargetingTest {
    private final World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
            (proxy, method, args) -> { throw new AssertionError(method.getName()); });
    private final MobArmyMatch.Side side = new MobArmyMatch.Side();

    @Test
    void existingTargetsAreReplacedWhenTheirDeathCooldownStarts() {
        Fighter resting = fighter(1);
        Fighter fighting = fighter(8);
        Enemy enemy = new Enemy(Mob.class, resting.player);
        side.roofTicks.put(resting.id, 200L);

        MobArmyTargeting.update(enemy.mob, side, List.of(resting.player, fighting.player), false);
        assertSame(fighting.player, enemy.target);
        side.roofTicks.put(fighting.id, 100L);
        MobArmyTargeting.update(enemy.mob, side, List.of(resting.player, fighting.player), false);
        assertNull(enemy.target);
        side.roofTicks.clear();
        assertTrue(MobArmyTargeting.canFight(side, resting.player, world));
    }

    @Test
    void deadOfflineSpectatingPreparingAndOtherTeamPlayersCannotBeTargets() {
        Fighter fighter = fighter(1);
        assertTrue(MobArmyTargeting.canFight(side, fighter.player, world));
        fighter.dead = true;
        assertFalse(MobArmyTargeting.canFight(side, fighter.player, world));
        fighter.dead = false;
        fighter.online = false;
        assertFalse(MobArmyTargeting.canFight(side, fighter.player, world));
        fighter.online = true;
        fighter.mode = GameMode.ADVENTURE;
        assertFalse(MobArmyTargeting.canFight(side, fighter.player, world));
        fighter.mode = GameMode.SPECTATOR;
        assertFalse(MobArmyTargeting.canFight(side, fighter.player, world));
        fighter.mode = GameMode.SURVIVAL;
        side.pendingBaseline.add(fighter.id);
        assertFalse(MobArmyTargeting.canFight(side, fighter.player, world));
        side.pendingBaseline.clear();
        side.preparing = true;
        assertFalse(MobArmyTargeting.canFight(side, fighter.player, world));
        side.preparing = false;
        side.finished = true;
        assertFalse(MobArmyTargeting.canFight(side, fighter.player, world));
        side.finished = false;
        side.members.clear();
        assertFalse(MobArmyTargeting.canFight(side, fighter.player, world));
    }

    @Test
    void vexesRefreshToTheNearestFighterAndImmediatelyRecoverLostAggro() {
        Fighter nearest = fighter(2);
        Fighter farther = fighter(10);
        Fighter resting = fighter(1);
        side.roofTicks.put(resting.id, 100L);
        Enemy enemy = new Enemy(Vex.class, farther.player);
        List<Player> players = List.of(resting.player, farther.player, nearest.player);

        MobArmyTargeting.update(enemy.mob, side, players, false);
        assertSame(farther.player, enemy.target);
        assertEquals(0, enemy.updates);
        MobArmyTargeting.update(enemy.mob, side, players, true);
        assertSame(nearest.player, enemy.target);
        MobArmyTargeting.update(enemy.mob, side, players, true);
        assertEquals(2, enemy.updates);
        enemy.target = null;
        MobArmyTargeting.update(enemy.mob, side, players, false);
        assertSame(nearest.player, enemy.target);
    }

    private Fighter fighter(double x) {
        Fighter fighter = new Fighter(x);
        side.members.put(fighter.id, "Fighter");
        return fighter;
    }

    private final class Fighter {
        final UUID id = UUID.randomUUID();
        boolean dead;
        boolean online = true;
        GameMode mode = GameMode.SURVIVAL;
        final Player player;

        Fighter(double x) {
            player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class}, (proxy, method, args) -> switch (method.getName()) {
                case "getUniqueId" -> id;
                case "getWorld" -> world;
                case "getLocation" -> new Location(world, x, 64, 0);
                case "isOnline" -> online;
                case "isDead" -> dead;
                case "getGameMode" -> mode;
                default -> throw new AssertionError(method.getName());
            });
        }
    }

    private final class Enemy {
        LivingEntity target;
        int updates;
        final Mob mob;

        Enemy(Class<? extends Mob> type, Player initialTarget) {
            target = initialTarget;
            mob = (Mob) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> switch (method.getName()) {
                case "getTarget" -> target;
                case "setTarget" -> { target = (LivingEntity) args[0]; updates++; yield null; }
                case "getWorld" -> world;
                case "getLocation" -> new Location(world, 0, 64, 0);
                default -> throw new AssertionError(method.getName());
            });
        }
    }
}
