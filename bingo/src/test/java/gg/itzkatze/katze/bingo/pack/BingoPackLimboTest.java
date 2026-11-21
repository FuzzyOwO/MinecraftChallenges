package gg.itzkatze.katze.bingo.pack;

import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BingoPackLimboTest {
    @Test void packAcceptanceDoesNotChangeLimboGameMode() throws Exception {
        UUID id = UUID.randomUUID();
        World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getKey")) return new NamespacedKey("katzegames", "limbo");
                    throw new AssertionError(method.getName());
                });
        GameMode[] current = {GameMode.ADVENTURE};
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> id;
                    case "getWorld" -> world;
                    case "isDead" -> false;
                    case "setGameMode" -> { current[0] = (GameMode) args[0]; yield null; }
                    default -> throw new AssertionError(method.getName());
                });
        BingoResourcePack pack = new BingoResourcePack();
        Field modesField = BingoResourcePack.class.getDeclaredField("modes");
        modesField.setAccessible(true);
        @SuppressWarnings("unchecked") Map<UUID, GameMode> modes = (Map<UUID, GameMode>) modesField.get(pack);
        modes.put(id, GameMode.SURVIVAL);
        Method release = BingoResourcePack.class.getDeclaredMethod("release", Player.class);
        release.setAccessible(true);
        release.invoke(pack, player);
        assertEquals(GameMode.ADVENTURE, current[0]);
    }
    @Test void packHoldDoesNotChangeLimboGameMode() throws Exception {
        UUID id = UUID.randomUUID();
        World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getKey")) return new NamespacedKey("katzegames", "limbo");
                    throw new AssertionError(method.getName());
                });
        GameMode[] current = {GameMode.ADVENTURE};
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> id;
                    case "getWorld" -> world;
                    case "getGameMode" -> current[0];
                    case "setGameMode" -> { current[0] = (GameMode) args[0]; yield null; }
                    default -> throw new AssertionError(method.getName());
                });
        BingoResourcePack pack = new BingoResourcePack();
        Field enabled = BingoResourcePack.class.getDeclaredField("enabled");
        enabled.setAccessible(true);
        enabled.setBoolean(pack, true);
        pack.hold(player);
        assertEquals(GameMode.ADVENTURE, current[0]);
    }
}
