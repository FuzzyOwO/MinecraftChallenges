package gg.itzkatze.katze.scoreboard;

import gg.itzkatze.katze.game.entry.GameEntryService;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScoreboardEntryTest {
    @Test void waitingParticipantSeesLobbyButAdmissionDoesNotReplaceGameBoard() {
        UUID id = UUID.randomUUID();
        GameEntryService service = new GameEntryService();
        service.beginRound(UUID.randomUUID(), Set.of(id));
        World limbo = world(new NamespacedKey("katzegames", "limbo"));
        World game = world(new NamespacedKey("katzegames", "game"));
        assertEquals(ScoreboardManager.EntrySidebar.LOBBY, ScoreboardManager.entrySidebar(service, id, limbo));
        assertEquals(ScoreboardManager.EntrySidebar.UNCHANGED, ScoreboardManager.entrySidebar(service, id, game));
    }

    private World world(NamespacedKey key) {
        return (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getKey")) return key;
                    throw new AssertionError(method.getName());
                });
    }
}
