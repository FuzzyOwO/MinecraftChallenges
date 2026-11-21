package gg.itzkatze.katze.spectator;

import gg.itzkatze.katze.challenges.ChallengeManager;
import gg.itzkatze.katze.game.entry.GameEntryService;
import gg.itzkatze.katze.player.KatzePlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SpectatorEntryTest {
    @Test void lateJoinIsMarkedWithoutTeleportOrInventoryChanges() throws Exception {
        UUID id = UUID.randomUUID();
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> id;
                    case "getName" -> "Late";
                    case "sendMessage" -> null;
                    default -> throw new AssertionError("Unexpected entry action: " + method.getName());
                });
        Field field = ChallengeManager.class.getDeclaredField("gameStarted");
        field.setAccessible(true);
        boolean previous = field.getBoolean(null);
        field.setBoolean(null, true);
        GameEntryService entries = GameEntryService.get();
        entries.beginRound(UUID.randomUUID(), Set.of(UUID.randomUUID()));
        entries.setRoutingEnabled(true);
        try {
            assertDoesNotThrow(() -> new SpectatorListener().onJoin(new PlayerJoinEvent(player, Component.empty())));
            assertEquals(SpectatorReason.LATE_JOIN, SpectatorManager.getReason(id));
        } finally {
            entries.setRoutingEnabled(false);
            entries.reset();
            field.setBoolean(null, previous);
            SpectatorManager.remove(id);
            KatzePlayer.remove(id);
        }
    }
}
