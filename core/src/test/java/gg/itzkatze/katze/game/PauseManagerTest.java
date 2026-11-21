package gg.itzkatze.katze.game;

import gg.itzkatze.katze.challenges.ChallengeManager;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import java.lang.reflect.Field;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.block.BlockFace;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class PauseManagerTest {
    private final World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
            (proxy, method, arguments) -> { if (method.getName().equals("getKey")) return NamespacedKey.minecraft("overworld"); throw new UnsupportedOperationException(method.getName()); });

    @Test void spectatorsCanFlyDuringAPauseAndDoNotKeepAnOldFrozenLocation() throws Exception {
        PauseManager manager = new PauseManager();
        var applied = PauseManager.class.getDeclaredField("pauseApplied");
        applied.setAccessible(true);
        applied.setBoolean(manager, true);
        boolean started = ChallengeManager.isGameStarted();
        ChallengeManager.markGameStarted();
        AtomicReference<GameMode> mode = new AtomicReference<>(GameMode.SURVIVAL);
        UUID id = UUID.randomUUID();
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "getGameMode" -> mode.get();
                    case "getUniqueId" -> id;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        try {
            PlayerMoveEvent survival = movement(player, 0, 1);
            manager.onPlayerMove(survival);
            assertEquals(0, survival.getTo().getX());

            mode.set(GameMode.SPECTATOR);
            PlayerMoveEvent spectator = movement(player, 10, 11);
            manager.onPlayerMove(spectator);
            assertEquals(11, spectator.getTo().getX());

            mode.set(GameMode.SURVIVAL);
            PlayerMoveEvent returned = movement(player, 11, 12);
            manager.onPlayerMove(returned);
            assertEquals(11, returned.getTo().getX());
        } finally {
            if (!started) ChallengeManager.resetGameState();
        }
    }

    @Test void pausedGameStillAllowsLimboMovement() throws Exception {
        PauseManager manager = new PauseManager();
        Field applied = PauseManager.class.getDeclaredField("pauseApplied");
        applied.setAccessible(true);
        applied.setBoolean(manager, true);
        boolean started = ChallengeManager.isGameStarted();
        ChallengeManager.markGameStarted();
        World limbo = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("getKey")) return new NamespacedKey("katzegames", "limbo");
                    throw new AssertionError(method.getName());
                });
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "getGameMode" -> GameMode.ADVENTURE;
                    case "getUniqueId" -> UUID.randomUUID();
                    default -> throw new AssertionError(method.getName());
                });
        try {
            PlayerMoveEvent movement = new PlayerMoveEvent(player, new Location(limbo, 0, 64, 0), new Location(limbo, 1, 64, 0));
            manager.onPlayerMove(movement);
            assertEquals(1, movement.getTo().getX());
        } finally {
            if (!started) ChallengeManager.resetGameState();
        }
    }
    @Test void pausedGameAllowsLimboInteraction() throws Exception {
        PauseManager manager = new PauseManager();
        Field applied = PauseManager.class.getDeclaredField("pauseApplied");
        applied.setAccessible(true);
        applied.setBoolean(manager, true);
        boolean started = ChallengeManager.isGameStarted();
        ChallengeManager.markGameStarted();
        World limbo = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("getKey")) return new NamespacedKey("katzegames", "limbo");
                    throw new AssertionError(method.getName());
                });
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("getWorld")) return limbo;
                    throw new AssertionError(method.getName());
                });
        try {
            PlayerInteractEvent interaction = new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, null, null, BlockFace.SELF);
            interaction.setUseItemInHand(org.bukkit.event.Event.Result.DEFAULT);
            manager.onInteract(interaction);
            assertNotEquals(org.bukkit.event.Event.Result.DENY, interaction.useItemInHand());
        } finally {
            if (!started) ChallengeManager.resetGameState();
        }
    }
    private PlayerMoveEvent movement(Player player, double from, double to) {
        return new PlayerMoveEvent(player, new Location(world, from, 64, 0), new Location(world, to, 64, 0));
    }
}
