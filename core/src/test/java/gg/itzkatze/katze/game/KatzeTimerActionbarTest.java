package gg.itzkatze.katze.game;

import gg.itzkatze.katze.player.KatzePlayer;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KatzeTimerActionbarTest {

    @Test void hidesActionbarForLobbyPlayers() {
        assertFalse(KatzeTimer.shouldShowActionbar(playerIn(world("limbo"))));
    }

    @Test void showsActionbarForPlayersInTheGameWorld() {
        assertTrue(KatzeTimer.shouldShowActionbar(playerIn(world("game"))));
    }

    private World world(String key) {
        NamespacedKey namespacedKey = new NamespacedKey("katzegames", key);
        return (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getKey")) return namespacedKey;
                    throw new AssertionError(method.getName());
                });
    }

    private KatzePlayer playerIn(World world) {
        UUID id = UUID.randomUUID();
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getWorld")) return world;
                    if (method.getName().equals("getUniqueId")) return id;
                    if (method.getName().equals("getName")) return "Tester";
                    throw new AssertionError(method.getName());
                });
        KatzePlayer katzePlayer = new KatzePlayer(id, "Tester") {
            @Override public Player getPlayer() {
                return player;
            }
        };
        KatzePlayer.PLAYERS.remove(katzePlayer);
        return katzePlayer;
    }
}
