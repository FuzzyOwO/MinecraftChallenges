package gg.itzkatze.katze.mobarmy;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityCombustByBlockEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent.Cause;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.entity.EntityTransformEvent.TransformReason;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MobArmyArenaListenerTest {
    @Test void fatigueProtectionTargetsGuardianAttacksAndAllowsOtherEffectSources() {
        assertTrue(MobArmyArenaListener.guardianEffect(EntityType.ELDER_GUARDIAN, Cause.ATTACK));
        assertTrue(MobArmyArenaListener.guardianEffect(null, Cause.ATTACK));
        assertFalse(MobArmyArenaListener.guardianEffect(EntityType.PLAYER, Cause.POTION_DRINK));
        assertFalse(MobArmyArenaListener.guardianEffect(null, Cause.PLUGIN));
        assertFalse(MobArmyArenaListener.guardianEffect(null, Cause.MILK));
    }

    @Test void zombificationKeepsTheOriginalMobTrackedAcrossAttempts() {
        MobArmyGame game = new MobArmy().game;
        game.match = new MobArmyMatch();
        MobArmyMatch.Side side = new MobArmyMatch.Side();
        game.match.sides.add(side);
        MobArmyListener listener = new MobArmyListener(game);

        for (EntityType type : List.of(EntityType.PIGLIN, EntityType.PIGLIN_BRUTE, EntityType.HOGLIN)) {
            UUID uuid = UUID.randomUUID();
            Entity mob = entity(uuid, type);
            EntityType replacementType = type == EntityType.HOGLIN ? EntityType.ZOGLIN : EntityType.ZOMBIFIED_PIGLIN;
            Entity replacement = entity(UUID.randomUUID(), replacementType);
            MobArmyMatch.LiveMob snapshot = new MobArmyMatch.LiveMob("snapshot", 0, 0, 0, 0, 0);
            side.live.put(uuid, snapshot);

            for (int attempt = 0; attempt < 2; attempt++) {
                EntityTransformEvent event = new EntityTransformEvent(mob, List.of(replacement), TransformReason.PIGLIN_ZOMBIFIED);
                listener.transform(event);
                assertTrue(event.isCancelled(), type.name());
                assertSame(snapshot, side.live.get(uuid), type.name());
            }
        }
    }

    @Test void transformationProtectionAllowsSlimeSplitsAndUntrackedMobs() {
        MobArmyGame game = new MobArmy().game;
        game.match = new MobArmyMatch();
        MobArmyMatch.Side side = new MobArmyMatch.Side();
        game.match.sides.add(side);
        MobArmyListener listener = new MobArmyListener(game);
        UUID slimeId = UUID.randomUUID();
        side.live.put(slimeId, new MobArmyMatch.LiveMob("snapshot", 0, 0, 0, 0, 0));

        EntityTransformEvent split = new EntityTransformEvent(entity(slimeId, EntityType.SLIME),
                List.of(entity(UUID.randomUUID(), EntityType.SLIME)), TransformReason.SPLIT);
        listener.transform(split);
        assertFalse(split.isCancelled());

        EntityTransformEvent untracked = new EntityTransformEvent(entity(UUID.randomUUID(), EntityType.PIGLIN),
                List.of(entity(UUID.randomUUID(), EntityType.ZOMBIFIED_PIGLIN)), TransformReason.PIGLIN_ZOMBIFIED);
        listener.transform(untracked);
        assertFalse(untracked.isCancelled());
    }

    private static Entity entity(UUID uuid, EntityType type) {
        return (Entity) Proxy.newProxyInstance(Entity.class.getClassLoader(), new Class<?>[]{Entity.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "getUniqueId" -> uuid;
                    case "getType" -> type;
                    default -> throw new AssertionError(method.getName());
                });
    }

    @Test void daylightDoesNotBurnWaveMobsButBlockFireStillWorks() {
        var challenge = new MobArmy();
        var game = challenge.game;
        game.match = new MobArmyMatch();
        var side = new MobArmyMatch.Side();
        game.match.sides.add(side);
        UUID uuid = UUID.randomUUID();
        LivingEntity mob = (LivingEntity) Proxy.newProxyInstance(LivingEntity.class.getClassLoader(), new Class<?>[]{LivingEntity.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("getUniqueId")) return uuid;
                    throw new AssertionError(method.getName());
                });
        side.live.put(uuid, new MobArmyMatch.LiveMob("snapshot", 0, 0, 0, 0, 0));
        var listener = new MobArmyArenaListener(game);
        var sun = new EntityCombustEvent(mob, 8f);
        listener.daylight(sun);
        assertTrue(sun.isCancelled());
        var lava = new EntityCombustByBlockEvent(null, mob, 8f);
        listener.daylight(lava);
        assertFalse(lava.isCancelled());
    }
}
