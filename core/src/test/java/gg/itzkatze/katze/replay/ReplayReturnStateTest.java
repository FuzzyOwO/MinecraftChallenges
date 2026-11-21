package gg.itzkatze.katze.replay;

import com.google.gson.Gson;
import gg.itzkatze.katze.util.JsonFiles;
import org.junit.jupiter.api.Test;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import gg.itzkatze.katze.game.entry.GameEntryService;
import org.bukkit.event.player.PlayerJoinEvent;
import net.kyori.adventure.text.Component;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReplayReturnStateTest {
    @Test void originalInventoryAndViewerFlagsSurviveRestartBeforeHotbarRestoration(@TempDir Path directory) throws Exception {
        var original = new ReplayService.ReturnState("katzegames:limbo", 4, 190, 8, 90, 0, "ADVENTURE", true, true,
                List.of("original-enchanted-item", "", "original-offhand"), 6, "original-cursor", false, false, true);
        Path file = directory.resolve("return.json");
        JsonFiles.write(file, original);
        assertEquals(original, JsonFiles.read(file, ReplayService.ReturnState.class));
    }

    @Test void olderReturnFilesRemainReadableWithoutReplacingThePlayersInventory() {
        var previous = new Gson().fromJson("""
                {"world":"katzegames:limbo","x":0,"y":190,"z":0,"yaw":0,"pitch":0,"mode":"ADVENTURE","flight":false,"flying":false}
                """, ReplayService.ReturnState.class);
        assertNull(previous.inventory());
        assertEquals("ADVENTURE", previous.mode());
    }
    @Test void recoveryInLimboRestoresReturnStateWithoutTeleporting() throws Exception {
        World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getKey")) return NamespacedKey.minecraft("overworld");
                    throw new AssertionError(method.getName());
                });
        Server server = (Server) Proxy.newProxyInstance(Server.class.getClassLoader(), new Class<?>[]{Server.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getWorld")) return world;
                    throw new AssertionError(method.getName());
                });
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getGameMode" -> GameMode.ADVENTURE;
                    case "setGameMode", "setAllowFlight", "setFlying" -> null;
                    default -> throw new AssertionError("Unexpected player action: " + method.getName());
                });
        ReplayService.ReturnState state = new ReplayService.ReturnState("minecraft:overworld", 4, 70, 8, 90, 0,
                "SURVIVAL", false, false, null, 0, null, false, false, true);
        Field field = Bukkit.class.getDeclaredField("server");
        field.setAccessible(true);
        Object previous = field.get(null);
        field.set(null, server);
        try {
            Location destination = state.restoreInLimbo(player);
            assertEquals(4, destination.getX());
            assertEquals(70, destination.getY());
        } finally {
            field.set(null, previous);
        }
    }
    @Test void sharedEntryDoesNotScheduleReplayJoinTeleport() {
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> { throw new AssertionError("Unexpected player access: " + method.getName()); });
        GameEntryService entries = GameEntryService.get();
        entries.setRoutingEnabled(true);
        try {
            assertDoesNotThrow(() -> new ReplayListener().onJoin(new PlayerJoinEvent(player, Component.empty())));
        } finally {
            entries.setRoutingEnabled(false);
        }
    }
}
