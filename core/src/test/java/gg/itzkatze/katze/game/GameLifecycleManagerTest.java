package gg.itzkatze.katze.game;

import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameLifecycleManagerTest {

    @Test void lobbyPlayersAreNotReturnedToTheLobbyOnReset() {
        assertFalse(GameLifecycleManager.shouldReturnToLobby(playerIn(world("limbo"))));
    }

    @Test void playersStillInTheGameAreReturnedToTheLobbyOnReset() {
        assertTrue(GameLifecycleManager.shouldReturnToLobby(playerIn(world("game"))));
    }

    private World world(String key) {
        NamespacedKey namespacedKey = new NamespacedKey("katzegames", key);
        return (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getKey")) return namespacedKey;
                    throw new AssertionError(method.getName());
                });
    }

    private Player playerIn(World world) {
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getWorld")) return world;
                    throw new AssertionError(method.getName());
                });
    }
}
