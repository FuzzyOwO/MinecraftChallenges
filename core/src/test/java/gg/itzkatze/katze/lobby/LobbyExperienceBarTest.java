package gg.itzkatze.katze.lobby;

import gg.itzkatze.katze.level.LevelCurve;
import gg.itzkatze.katze.level.LevelStore;
import gg.itzkatze.katze.player.data.PlayerDataStore;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LobbyExperienceBarTest {
    @TempDir Path directory;
    private final UUID id = UUID.randomUUID();
    private final World lobby = world("limbo");
    private final World game = world("game");
    private final List<Bar> sent = new ArrayList<>();

    @Test void showsKatzeGamesLevelAndFractionInTheLobby() {
        LevelStore levels = levels();
        int cost = LevelCurve.costOf(12);
        int earned = cost / 3;
        levels.adjust(id, "Player", (int) LevelCurve.totalFor(12) + earned);
        LobbyExperienceBar bar = new LobbyExperienceBar(levels);
        Player player = player(lobby);

        bar.refresh(player);

        assertEquals(List.of(new Bar((float) earned / cost, 12)), sent);
    }

    @Test void showsAFullBarAtTheLevelCap() {
        LevelStore levels = levels();
        levels.adjust(id, "Player", Integer.MAX_VALUE);

        new LobbyExperienceBar(levels).refresh(player(lobby));

        assertEquals(List.of(new Bar(1f, LevelCurve.MAX_LEVEL)), sent);
    }

    @Test void leavingTheLobbyRestoresMinecraftExperience() {
        LobbyExperienceBar bar = new LobbyExperienceBar(levels());
        Player player = player(game);

        bar.refresh(player);
        assertTrue(sent.isEmpty());
        bar.onWorldChange(new PlayerChangedWorldEvent(player, lobby));

        assertEquals(List.of(new Bar(0.25f, 7)), sent);
    }

    private LevelStore levels() {
        return new LevelStore(new PlayerDataStore(directory.resolve("players.json"), (task, ticks) -> {}));
    }

    private Player player(World world) {
        return proxy(Player.class, (instance, method, args) -> switch (method.getName()) {
            case "getWorld" -> world;
            case "getUniqueId" -> id;
            case "getExp" -> 0.25f;
            case "getLevel" -> 7;
            case "sendExperienceChange" -> { sent.add(new Bar((float) args[0], (int) args[1])); yield null; }
            default -> throw new AssertionError(method.getName());
        });
    }

    private static World world(String name) {
        return proxy(World.class, (instance, method, args) -> {
            if (method.getName().equals("getKey")) return new NamespacedKey("katzegames", name);
            throw new AssertionError(method.getName());
        });
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }

    private record Bar(float progress, int level) {}
}
