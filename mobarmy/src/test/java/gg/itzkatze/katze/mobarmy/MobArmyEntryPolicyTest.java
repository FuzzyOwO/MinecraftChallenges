package gg.itzkatze.katze.mobarmy;

import gg.itzkatze.katze.game.entry.EntryReason;
import gg.itzkatze.katze.game.entry.GameEntryPolicy;
import org.junit.jupiter.api.Test;
import gg.itzkatze.katze.game.entry.GameEntryService;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.entity.Player;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class MobArmyEntryPolicyTest {
    @Test void spectatorAdmissionDoesNotInitializeParticipantState() {
        GameEntryPolicy policy = new MobArmy().getEntryPolicy();
        assertDoesNotThrow(() -> policy.admitted(UUID.randomUUID(), EntryReason.SPECTATE));
    }
    @Test void onlyLiveRosterCanRejoin() {
        UUID participant = UUID.randomUUID();
        MobArmy mode = new MobArmy();
        mode.game.match = new MobArmyMatch();
        MobArmyMatch.Side side = new MobArmyMatch.Side();
        side.id = "side";
        side.members.put(participant, "Player");
        mode.game.match.sides.add(side);
        GameEntryPolicy policy = mode.getEntryPolicy();
        assertEquals(200, policy.priority());
        assertFalse(policy.prepare(UUID.randomUUID(), EntryReason.REJOIN).allowed());
        assertTrue(policy.prepare(participant, EntryReason.REJOIN).allowed());
        side.finished = true;
        assertFalse(policy.prepare(participant, EntryReason.REJOIN).allowed());
    }
    @Test void waitingPlayerIsNotTargetedByArenaActions() throws Exception {
        UUID id = UUID.randomUUID();
        MobArmyMatch.Side side = new MobArmyMatch.Side();
        side.members.put(id, "Player");
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> { throw new AssertionError(method.getName()); });
        Server server = (Server) Proxy.newProxyInstance(Server.class.getClassLoader(), new Class<?>[]{Server.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getPlayer")) return player;
                    throw new AssertionError(method.getName());
                });
        Field field = Bukkit.class.getDeclaredField("server");
        field.setAccessible(true);
        Object previous = field.get(null);
        field.set(null, server);
        GameEntryService service = GameEntryService.get();
        service.beginRound(UUID.randomUUID(), Set.of(id));
        AtomicInteger actions = new AtomicInteger();
        try {
            MobArmyGame.online(side, ignored -> actions.incrementAndGet());
            assertEquals(0, actions.get());
        } finally {
            service.reset();
            field.set(null, previous);
        }
    }
    @Test void delayedJoinDoesNotPullWaitingPlayerIntoArena() {
        UUID id = UUID.randomUUID();
        MobArmy mode = new MobArmy();
        mode.game.match = new MobArmyMatch();
        mode.game.match.phase = MobArmyMatch.Phase.CONFIGURATION;
        MobArmyMatch.Side side = new MobArmyMatch.Side();
        side.id = "side";
        side.members.put(id, "Player");
        mode.game.match.sides.add(side);
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getUniqueId")) return id;
                    throw new AssertionError("Unexpected arena action: " + method.getName());
                });
        GameEntryService service = GameEntryService.get();
        service.beginRound(UUID.randomUUID(), Set.of(id));
        try {
            assertDoesNotThrow(() -> mode.game.join(player));
        } finally {
            service.reset();
        }
    }
    @Test void waitingPlayerRespawnIsNotRedirectedToArena() {
        UUID id = UUID.randomUUID();
        MobArmy mode = new MobArmy();
        mode.game.match = new MobArmyMatch();
        mode.game.match.phase = MobArmyMatch.Phase.CONFIGURATION;
        MobArmyMatch.Side side = new MobArmyMatch.Side();
        side.id = "side";
        side.members.put(id, "Player");
        mode.game.match.sides.add(side);
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> { if (method.getName().equals("getUniqueId")) return id; throw new AssertionError(method.getName()); });
        World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> { throw new AssertionError(method.getName()); });
        Location limbo = new Location(world, 0, 190, 0);
        PlayerRespawnEvent event = new PlayerRespawnEvent(player, limbo, false, false, false, PlayerRespawnEvent.RespawnReason.DEATH);
        GameEntryService service = GameEntryService.get();
        service.beginRound(UUID.randomUUID(), Set.of(id));
        try {
            assertDoesNotThrow(() -> new MobArmyListener(mode.game).respawn(event));
            assertEquals(190, event.getRespawnLocation().getY());
        } finally {
            service.reset();
        }
    }
    @Test void waitingPlayerCanMoveOutsideArena() {
        UUID id = UUID.randomUUID();
        MobArmy mode = new MobArmy();
        mode.game.match = new MobArmyMatch();
        mode.game.match.phase = MobArmyMatch.Phase.CONFIGURATION;
        MobArmyMatch.Side side = new MobArmyMatch.Side();
        side.id = "side";
        side.members.put(id, "Player");
        mode.game.match.sides.add(side);
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> { if (method.getName().equals("getUniqueId")) return id; throw new AssertionError(method.getName()); });
        World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> { throw new AssertionError(method.getName()); });
        PlayerMoveEvent event = new PlayerMoveEvent(player, new Location(world, 0, 190, 0), new Location(world, 1, 190, 0));
        GameEntryService service = GameEntryService.get();
        service.beginRound(UUID.randomUUID(), Set.of(id));
        try {
            assertDoesNotThrow(() -> new MobArmyListener(mode.game).move(event));
            assertEquals(1, event.getTo().getX());
        } finally {
            service.reset();
        }
    }
}
