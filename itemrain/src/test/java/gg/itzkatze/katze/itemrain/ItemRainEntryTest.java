package gg.itzkatze.katze.itemrain;

import gg.itzkatze.katze.game.entry.GameEntryService;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ItemRainEntryTest {
    @Test void waitingParticipantDoesNotReceiveItems() {
        UUID id = UUID.randomUUID();
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getUniqueId")) return id;
                    throw new AssertionError(method.getName());
                });
        GameEntryService entries = GameEntryService.get();
        entries.beginRound(UUID.randomUUID(), Set.of(id));
        try {
            assertFalse(ItemRainGameListener.canReceive(player));
        } finally {
            entries.reset();
        }
    }
}
