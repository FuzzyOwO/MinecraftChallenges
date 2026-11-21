package gg.itzkatze.katze.deathswap;

import gg.itzkatze.katze.game.entry.GameEntryService;
import gg.itzkatze.katze.spectator.SpectatorManager;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DeathSwapEntryTest {
    @Test void shutdownQuitDoesNotEliminatePlayer() throws Exception {
        UUID id = UUID.randomUUID();
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getUniqueId")) return id;
                    throw new AssertionError(method.getName());
                });
        DeathSwapGameListener listener = new DeathSwapGameListener();
        Field started = DeathSwapGameListener.class.getDeclaredField("started");
        started.setAccessible(true);
        started.setBoolean(listener, true);
        GameEntryService entries = GameEntryService.get();
        entries.reset();
        entries.shutdown();
        try {
            assertDoesNotThrow(() -> listener.onPlayerQuit(new PlayerQuitEvent(player, Component.empty(), PlayerQuitEvent.QuitReason.DISCONNECTED)));
            assertFalse(SpectatorManager.isSpectator(id));
        } finally {
            entries.reset();
            SpectatorManager.remove(id);
        }
    }
}
