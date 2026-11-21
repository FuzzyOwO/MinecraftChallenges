package gg.itzkatze.katze.player;

import gg.itzkatze.katze.game.entry.GameEntryService;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertFalse;

class AfkTrackerEntryTest {
    @Test void waitingParticipantDoesNotBecomeAfkForTheRound() throws Exception {
        UUID id = UUID.randomUUID();
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getUniqueId")) return id;
                    if (method.getName().equals("sendMessage")) return null;
                    throw new AssertionError(method.getName());
                });
        Server server = (Server) Proxy.newProxyInstance(Server.class.getClassLoader(), new Class<?>[]{Server.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getOnlinePlayers")) return List.of(player);
                    throw new AssertionError(method.getName());
                });
        Field field = Bukkit.class.getDeclaredField("server");
        field.setAccessible(true);
        Object previous = field.get(null);
        field.set(null, server);
        GameEntryService entries = GameEntryService.get();
        entries.beginRound(UUID.randomUUID(), Set.of(id));
        AtomicLong clock = new AtomicLong();
        AfkTracker tracker = new AfkTracker(clock::get);
        try {
            tracker.tick();
            clock.set(AfkTracker.IDLE_MILLIS + 1);
            tracker.tick();
            assertFalse(tracker.isAfk(id));
        } finally {
            entries.reset();
            field.set(null, previous);
        }
    }
}
