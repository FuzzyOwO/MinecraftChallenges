package gg.itzkatze.katze.game;

import gg.itzkatze.katze.challenges.ChallengeManager;
import gg.itzkatze.katze.game.entry.GameEntryService;
import gg.itzkatze.katze.lobby.LobbyManager;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LimboRespawnTest {
    @Test void waitingPlayerRespawnsInLimboDuringRound() throws Exception {
        UUID id = UUID.randomUUID();
        World limbo = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getKey")) return new NamespacedKey("katzegames", "limbo");
                    throw new AssertionError(method.getName());
                });
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> id;
                    case "getWorld" -> limbo;
                    default -> throw new AssertionError(method.getName());
                });
        Field spawnField = LobbyManager.class.getDeclaredField("fallbackSpawn");
        spawnField.setAccessible(true);
        Object oldSpawn = spawnField.get(null);
        spawnField.set(null, new Location(limbo, 1, 190, 2));
        Field startedField = ChallengeManager.class.getDeclaredField("gameStarted");
        startedField.setAccessible(true);
        boolean oldStarted = startedField.getBoolean(null);
        startedField.setBoolean(null, true);
        GameEntryService service = GameEntryService.get();
        service.beginRound(UUID.randomUUID(), Set.of(id));
        try {
            PlayerRespawnEvent event = new PlayerRespawnEvent(player, new Location(limbo, 4, 60, 4), false, false, false,
                    PlayerRespawnEvent.RespawnReason.DEATH);
            new GameWorldListener().onRespawn(event);
            assertEquals(190, event.getRespawnLocation().getY());
        } finally {
            service.reset();
            startedField.setBoolean(null, oldStarted);
            spawnField.set(null, oldSpawn);
        }
    }
}
