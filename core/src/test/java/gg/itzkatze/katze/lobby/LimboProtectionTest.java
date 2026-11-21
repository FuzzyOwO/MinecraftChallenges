package gg.itzkatze.katze.lobby;

import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LimboProtectionTest {
    @Test void limboProtectsHungerWithoutAnActiveIsland() {
        World limbo = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getKey")) return new NamespacedKey("katzegames", "limbo");
                    throw new AssertionError(method.getName());
                });
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getWorld")) return limbo;
                    throw new AssertionError(method.getName());
                });
        FoodLevelChangeEvent event = new FoodLevelChangeEvent(player, 10, null);
        new LobbyManager().onHungerChange(event);
        assertTrue(event.isCancelled());
    }
    @Test void missingIslandHasSafeFallbackPlatform() {
        AtomicInteger floorBlocks = new AtomicInteger();
        Block block = (Block) Proxy.newProxyInstance(Block.class.getClassLoader(), new Class<?>[]{Block.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("setType")) {
                        if (args[0] == Material.BEDROCK) floorBlocks.incrementAndGet();
                        return null;
                    }
                    throw new AssertionError(method.getName());
                });
        World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getBlockAt" -> block;
                    case "setSpawnLocation" -> true;
                    default -> throw new AssertionError(method.getName());
                });
        Location spawn = LobbyManager.createFallback(world);
        assertEquals(25, floorBlocks.get());
        assertEquals(190, spawn.getY());
    }
}
