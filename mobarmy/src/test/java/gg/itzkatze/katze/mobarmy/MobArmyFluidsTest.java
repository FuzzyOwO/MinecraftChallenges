package gg.itzkatze.katze.mobarmy;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class MobArmyFluidsTest {
    @Test void lavaReactionsCanCompleteAndTheirOriginalFluidIsSaved() {
        for (Material result : new Material[]{Material.STONE, Material.COBBLESTONE, Material.OBSIDIAN, Material.BASALT}) {
            var fixture = new Fixture();
            var event = fixture.form(Material.LAVA, result, 10, 90);
            fixture.listener.form(event);
            assertFalse(event.isCancelled(), result.name());
            assertEquals(1, fixture.side.blocks.size());
            assertEquals("minecraft:lava", fixture.side.blocks.getFirst().original());
            assertTrue(fixture.side.blocks.getFirst().placed());
        }
        var fixture = new Fixture();
        var event = fixture.form(Material.WATER, Material.STONE, 10, 90);
        fixture.listener.form(event);
        assertFalse(event.isCancelled());
        assertEquals("minecraft:water", fixture.side.blocks.getFirst().original());
    }

    @Test void unrelatedFormationsAndArenaBoundariesStayProtected() {
        var fixture = new Fixture();
        for (var event : new BlockFormEvent[]{fixture.form(Material.STONE, Material.COBBLESTONE, 10, 90),
                fixture.form(Material.WATER, Material.ICE, 10, 90), fixture.form(Material.LAVA, Material.OBSIDIAN, -1, 90),
                fixture.form(Material.LAVA, Material.OBSIDIAN, 10, MobArmyArena.FLOOR)}) {
            fixture.listener.form(event);
            assertTrue(event.isCancelled());
        }
        assertTrue(fixture.side.blocks.isEmpty());
    }

    @Test void fluidsStayFrozenDuringConfigurationPreparationAndAfterFinishing() {
        var fixture = new Fixture();
        fixture.game.match.phase = MobArmyMatch.Phase.CONFIGURATION;
        var configuration = fixture.form(Material.LAVA, Material.OBSIDIAN, 10, 90);
        fixture.listener.form(configuration);
        assertTrue(configuration.isCancelled());
        fixture.game.match.phase = MobArmyMatch.Phase.COMBAT;
        fixture.side.preparing = true;
        var preparing = fixture.form(Material.LAVA, Material.OBSIDIAN, 10, 90);
        fixture.listener.form(preparing);
        assertTrue(preparing.isCancelled());
        fixture.side.preparing = false;
        fixture.side.finished = true;
        var finished = fixture.form(Material.LAVA, Material.OBSIDIAN, 10, 90);
        fixture.listener.form(finished);
        assertTrue(finished.isCancelled());
        assertTrue(fixture.side.blocks.isEmpty());
    }

    @Test void normalFlowIsTrackedAndCannotLeaveTheArena() {
        var fixture = new Fixture();
        Block source = fixture.block(Material.LAVA, 10, 90);
        var inside = new BlockFromToEvent(source, fixture.block(Material.AIR, 11, 90));
        fixture.listener.flow(inside);
        assertFalse(inside.isCancelled());
        assertEquals(1, fixture.side.blocks.size());
        var outside = new BlockFromToEvent(source, fixture.block(Material.AIR, -1, 90));
        fixture.listener.flow(outside);
        assertTrue(outside.isCancelled());
        assertEquals(1, fixture.side.blocks.size());
    }

    private static final class Fixture {
        private final MobArmyGame game = new MobArmy().game;
        private final MobArmyMatch.Side side = new MobArmyMatch.Side();
        private final MobArmyListener listener = new MobArmyListener(game);
        private final World world = proxy(World.class, Map.of("getKey", MobArmyArena.worldKey(0)));

        private Fixture() {
            game.match = new MobArmyMatch();
            game.match.phase = MobArmyMatch.Phase.COMBAT;
            game.match.sides.add(side);
        }

        private BlockFormEvent form(Material original, Material result, int x, int y) {
            return new BlockFormEvent(block(original, x, y), proxy(BlockState.class, Map.of("getType", result)));
        }

        private Block block(Material material, int x, int y) {
            BlockData data = proxy(BlockData.class, Map.of("getAsString", "minecraft:" + material.name().toLowerCase(Locale.ROOT)));
            return proxy(Block.class, Map.of("getWorld", world, "getLocation", new Location(world, x, y, 10),
                    "getX", x, "getY", y, "getZ", 10, "getType", material, "getBlockData", data,
                    "isLiquid", material == Material.LAVA || material == Material.WATER));
        }
    }

    private static <T> T proxy(Class<T> type, Map<String, Object> values) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (instance, method, args) -> {
            if (values.containsKey(method.getName())) return values.get(method.getName());
            throw new AssertionError(method.getName());
        }));
    }
}
