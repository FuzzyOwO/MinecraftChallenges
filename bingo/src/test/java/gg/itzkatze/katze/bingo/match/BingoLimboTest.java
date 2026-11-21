package gg.itzkatze.katze.bingo.match;

import gg.itzkatze.katze.bingo.board.BingoObjectiveCatalog;
import gg.itzkatze.katze.game.entry.GameEntryService;
import java.util.Set;
import gg.itzkatze.katze.bingo.pack.BingoResourcePack;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;

class BingoLimboTest {
    @Test void packWaitDoesNotFreezeLimbo() {
        World limbo = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getKey")) return new NamespacedKey("katzegames", "limbo");
                    throw new AssertionError(method.getName());
                });
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getWorld" -> limbo;
                    case "getUniqueId" -> UUID.randomUUID();
                    default -> throw new AssertionError(method.getName());
                });
        BingoMatchManager matches = new BingoMatchManager(new BingoObjectiveCatalog(), new BingoResourcePack());
        assertFalse(matches.frozen(player));
    }
    @Test void waitingPlayerCannotProgressBingo() {
        UUID id = UUID.randomUUID();
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getUniqueId")) return id;
                    throw new AssertionError("A waiting player was examined for gameplay: " + method.getName());
                });
        GameEntryService service = GameEntryService.get();
        service.beginRound(UUID.randomUUID(), Set.of(id));
        try {
            BingoMatchManager matches = new BingoMatchManager(new BingoObjectiveCatalog(), new BingoResourcePack());
            assertFalse(matches.eligible(player));
        } finally {
            service.reset();
        }
    }
}
