package gg.itzkatze.katze.mobarmy;

import org.bukkit.ExplosionResult;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MobArmyExplosionsTest {
    private final MobArmyGame game = new MobArmy().game;
    private final MobArmyMatch.Side side = new MobArmyMatch.Side();
    private final MobArmyListener listener = new MobArmyListener(game);
    private final World world = proxy(World.class, Map.of("getKey", MobArmyArena.worldKey(0)));

    MobArmyExplosionsTest() {
        game.match = new MobArmyMatch();
        game.match.phase = MobArmyMatch.Phase.COMBAT;
        game.match.sides.add(side);
    }

    @Test void explodingTntLeavesOnlyPlayerPlacedTntForTheServerToPrime() {
        Block placedTnt = placedBlock(Material.TNT, 10);
        Block placedStone = placedBlock(Material.STONE, 11);
        Block templateTnt = block(Material.TNT, 12);
        Block outsideTnt = placedBlock(Material.TNT, -1);
        EntityExplodeEvent event = explosion(TNTPrimed.class, placedStone, placedTnt, templateTnt, outsideTnt);

        listener.explode(event);

        assertFalse(event.isCancelled());
        assertEquals(0f, event.getYield());
        assertEquals(1, event.blockList().size());
        assertSame(placedTnt, event.blockList().getFirst());
    }

    @Test void otherExplosionsAndInactiveArenasKeepTntProtected() {
        Block tnt = placedBlock(Material.TNT, 10);
        EntityExplodeEvent creeper = explosion(Creeper.class, tnt);
        listener.explode(creeper);
        assertTrue(creeper.blockList().isEmpty());

        side.preparing = true;
        EntityExplodeEvent inactive = explosion(TNTPrimed.class, tnt);
        listener.explode(inactive);
        assertTrue(inactive.blockList().isEmpty());
    }

    private Block placedBlock(Material material, int x) {
        side.blocks.add(new MobArmyMatch.BlockChange(x, 90, 10, "minecraft:air", true));
        return block(material, x);
    }

    private Block block(Material material, int x) {
        return proxy(Block.class, Map.of("getType", material, "getLocation", new Location(world, x, 90, 10),
                "getX", x, "getY", 90, "getZ", 10));
    }

    private EntityExplodeEvent explosion(Class<? extends Entity> type, Block... blocks) {
        return new EntityExplodeEvent(proxy(type, Map.of()), new Location(world, 9, 90, 10),
                new ArrayList<>(List.of(blocks)), 1f, ExplosionResult.DESTROY);
    }

    private static <T> T proxy(Class<T> type, Map<String, Object> values) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (instance, method, args) -> {
            if (values.containsKey(method.getName())) return values.get(method.getName());
            throw new AssertionError(method.getName());
        }));
    }
}
