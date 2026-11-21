package gg.itzkatze.katze.game.entry;

import org.bukkit.entity.Player;
import com.google.gson.Gson;
import gg.itzkatze.katze.spectator.SpectatorManager;
import gg.itzkatze.katze.game.GameTimer;
import gg.itzkatze.katze.challenges.Challenge;
import gg.itzkatze.katze.challenges.ChallengeManager;
import gg.itzkatze.katze.challenges.ChallengeType;
import gg.itzkatze.katze.commands.framework.KatzeCommand;
import org.bukkit.event.Listener;
import java.util.HashSet;
import gg.itzkatze.katze.game.TimerRegistry;
import gg.itzkatze.katze.spectator.SpectatorReason;
import org.bukkit.Location;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.PlayerInventory;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameEntryServiceTest {
    @Test void staleRoundCannotCompleteTeleport() {
        UUID participant = UUID.randomUUID();
        UUID oldRound = UUID.randomUUID();
        GameEntryService service = new GameEntryService();
        service.beginRound(UUID.randomUUID(), Set.of(participant));
        assertEquals(EntryResult.DENIED, service.enter(player(participant), oldRound, EntryReason.REJOIN));
    }

    @Test void lateJoinCannotBecomeParticipant() {
        UUID round = UUID.randomUUID();
        GameEntryService service = new GameEntryService();
        service.beginRound(round, Set.of(UUID.randomUUID()));
        assertEquals(EntryResult.DENIED, service.enter(player(UUID.randomUUID()), round, EntryReason.REJOIN));
    }

    @Test void eligibleRoundStartIsAdmittedOnce() {
        UUID id = UUID.randomUUID();
        UUID round = UUID.randomUUID();
        AtomicInteger admissions = new AtomicInteger();
        GameEntryPolicy policy = new GameEntryPolicy() {
            public int priority() { return 1; }
            public Admission prepare(UUID playerId, EntryReason reason) {
                return new Admission(true, new Location(null, 1, 2, 3), null);
            }
            public void admitted(UUID playerId, EntryReason reason) { admissions.incrementAndGet(); }
            public void left(UUID playerId, LeaveReason reason) {}
            public List<Location> spectatorTargets() { return List.of(); }
        };
        GameEntryService service = new GameEntryService(policy);
        service.beginRound(round, Set.of(id));
        Player player = roundStartPlayer(id);
        assertEquals(EntryResult.ENTERED, service.enter(player, round, EntryReason.ROUND_START));
        assertEquals(EntryResult.DENIED, service.enter(player, round, EntryReason.ROUND_START));
        assertEquals(1, admissions.get());
    }
    @Test void interruptedPreparationPersistsAlreadyAdmittedParticipants() {
        UUID id = UUID.randomUUID();
        UUID round = UUID.randomUUID();
        GameEntryPolicy entryPolicy = new GameEntryPolicy() {
            public int priority() { return 1; }
            public Admission prepare(UUID playerId, EntryReason reason) { return new Admission(true, new Location(null, 0, 70, 0), null); }
            public void admitted(UUID playerId, EntryReason reason) {}
            public void left(UUID playerId, LeaveReason reason) {}
            public List<Location> spectatorTargets() { return List.of(); }
        };
        GameEntryService service = new GameEntryService(entryPolicy);
        service.beginRound(round, Set.of(id));
        assertEquals(EntryResult.ENTERED, service.enter(roundStartPlayer(id), round, EntryReason.ROUND_START));
        GameEntryService.Snapshot saved = new Gson().fromJson(new Gson().toJson(service.snapshot()), GameEntryService.Snapshot.class);
        GameEntryService recovered = new GameEntryService(entryPolicy);
        recovered.restore(saved);
        assertTrue(recovered.snapshot().initialAdmitted().contains(id));
        assertFalse(recovered.snapshot().initialAdmissionComplete());
    }
    @Test void offlineParticipantReceivesInitialAdmissionAfterPreparation() {
        UUID id = UUID.randomUUID();
        UUID round = UUID.randomUUID();
        AtomicReference<EntryReason> admitted = new AtomicReference<>();
        GameEntryPolicy entryPolicy = new GameEntryPolicy() {
            public int priority() { return 1; }
            public Admission prepare(UUID playerId, EntryReason reason) { return new Admission(true, new Location(null, 0, 70, 0), null); }
            public void admitted(UUID playerId, EntryReason reason) { admitted.set(reason); }
            public void left(UUID playerId, LeaveReason reason) {}
            public List<Location> spectatorTargets() { return List.of(); }
        };
        GameEntryService service = new GameEntryService(entryPolicy);
        service.restore(new GameEntryService.Snapshot(1, round, Set.of(id), Map.of(id, new SavedGamePlayerState()), Map.of(), true, Set.of()));
        assertEquals(EntryResult.ENTERED, service.rejoin(roundStartPlayer(id)));
        assertEquals(EntryReason.ROUND_START, admitted.get());
    }
    @Test void equalHighestPolicyPrioritiesAreRejected() {
        GameEntryPolicy first = policy(100);
        GameEntryPolicy second = policy(100);
        assertThrows(IllegalStateException.class, () -> GameEntryService.selectPolicy(List.of(first, second)));
    }

    @Test void eliminationStillPreventsPlayingAfterPresenceTransition() {
        UUID id = UUID.randomUUID();
        UUID round = UUID.randomUUID();
        GameEntryPolicy policy = new GameEntryPolicy() {
            public int priority() { return 1; }
            public Admission prepare(UUID playerId, EntryReason reason) { return new Admission(true, new Location(null, 0, 0, 0), null); }
            public void admitted(UUID playerId, EntryReason reason) {}
            public void left(UUID playerId, LeaveReason reason) {}
            public List<Location> spectatorTargets() { return List.of(); }
        };
        GameEntryService service = new GameEntryService(policy);
        service.beginRound(round, Set.of(id));
        Player player = roundStartPlayer(id);
        assertEquals(EntryResult.ENTERED, service.enter(player, round, EntryReason.ROUND_START));
        SpectatorManager.restore(id, SpectatorReason.ELIMINATED);
        try {
            assertFalse(service.isPlaying(id));
        } finally {
            SpectatorManager.remove(id);
        }
    }
    @Test void resetInvalidatesOldRequests() {
        UUID id = UUID.randomUUID();
        UUID round = UUID.randomUUID();
        GameEntryService service = new GameEntryService(policy(1));
        service.beginRound(round, Set.of(id));
        service.reset();
        assertEquals(EntryResult.DENIED, service.enter(player(id), round, EntryReason.REJOIN));
        assertFalse(service.isPlaying(id));
    }
    @Test void snapshotKeepsRoundIdentityAndEligibility() {
        UUID id = UUID.randomUUID();
        UUID round = UUID.randomUUID();
        GameEntryService service = new GameEntryService(policy(1));
        service.beginRound(round, Set.of(id));
        Gson gson = new Gson();
        GameEntryService.Snapshot snapshot = gson.fromJson(gson.toJson(service.snapshot()), GameEntryService.Snapshot.class);
        GameEntryService restored = new GameEntryService(policy(1));
        restored.restore(snapshot);
        assertEquals(round, restored.currentRoundId());
        assertEquals(EntryResult.DENIED, restored.enter(player(UUID.randomUUID()), round, EntryReason.REJOIN));
    }
    @Test void rejoinRestoresExactlyOnce() throws Exception {
        UUID id = UUID.randomUUID();
        UUID round = UUID.randomUUID();
        SavedGamePlayerState saved = new SavedGamePlayerState();
        saved.inventorySize = 41;
        saved.inventory = new ArrayList<>(Collections.nCopies(41, null));
        saved.cursor = new ArrayList<>(Collections.singletonList(null));
        saved.effects = List.of();
        saved.world = "minecraft:overworld";
        saved.gameMode = "SURVIVAL";
        saved.health = 20;
        World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> { if (method.getName().equals("getKey")) return NamespacedKey.minecraft("overworld"); throw new AssertionError(method.getName()); });
        Server server = (Server) Proxy.newProxyInstance(Server.class.getClassLoader(), new Class<?>[]{Server.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getWorld")) return world;
                    throw new AssertionError(method.getName());
                });
        AtomicInteger replacements = new AtomicInteger();
        PlayerInventory inventory = (PlayerInventory) Proxy.newProxyInstance(PlayerInventory.class.getClassLoader(), new Class<?>[]{PlayerInventory.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("setContents")) replacements.incrementAndGet();
                    if (method.getName().equals("getContents")) return new org.bukkit.inventory.ItemStack[41];
                    if (method.getName().equals("getSize")) return 41;
                    if (method.getName().equals("getHeldItemSlot")) return 0;
                    return null;
                });
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getUniqueId")) return id;
                    if (method.getName().equals("getInventory")) return inventory;
                    if (method.getName().equals("getActivePotionEffects")) return List.of();
                    if (method.getName().equals("getLocation")) return new Location(world, 0, 70, 0);
                    if (method.getName().equals("getGameMode")) return org.bukkit.GameMode.ADVENTURE;
                    if (method.getName().equals("getHealth")) return 20d;
                    if (method.getName().equals("getAbsorptionAmount")) return 0d;
                    if (List.of("getSaturation", "getExhaustion", "getExp").contains(method.getName())) return 0f;
                    if (List.of("getFoodLevel", "getLevel", "getTotalExperience", "getFireTicks", "getFreezeTicks", "getRemainingAir").contains(method.getName())) return 0;
                    if (method.getName().equals("teleport")) return true;
                    return null;
                });
        GameEntryService service = new GameEntryService();
        service.restore(new GameEntryService.Snapshot(1, round, Set.of(id), Map.of(id, saved)));
        Field field = Bukkit.class.getDeclaredField("server");
        field.setAccessible(true);
        Object previous = field.get(null);
        field.set(null, server);
        try {
            assertEquals(EntryResult.ENTERED, service.enter(player, round, EntryReason.REJOIN));
            assertEquals(EntryResult.DENIED, service.enter(player, round, EntryReason.REJOIN));
            assertEquals(1, replacements.get());
        } finally {
            field.set(null, previous);
        }
    }
    @Test void pausedRoundQueuesRejoinWithoutRestoring() {
        UUID id = UUID.randomUUID();
        UUID round = UUID.randomUUID();
        GameEntryService service = new GameEntryService();
        service.restore(new GameEntryService.Snapshot(1, round, Set.of(id), Map.of(id, new SavedGamePlayerState())));
        GameTimer timer = TimerRegistry.getTimer();
        GameTimer.Mode previousMode = timer.getMode();
        try {
            timer.setMode(GameTimer.Mode.COUNT_UP);
            timer.pause();
            assertEquals(EntryResult.QUEUED, service.enter(player(id), round, EntryReason.REJOIN));
        } finally {
            timer.setMode(previousMode);
        }
    }
    @Test void disconnectCapturesPlayingStateAndEndsPresence() {
        UUID id = UUID.randomUUID();
        UUID round = UUID.randomUUID();
        World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getKey")) return NamespacedKey.minecraft("overworld");
                    throw new AssertionError(method.getName());
                });
        PlayerInventory inventory = (PlayerInventory) Proxy.newProxyInstance(PlayerInventory.class.getClassLoader(), new Class<?>[]{PlayerInventory.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getContents" -> new org.bukkit.inventory.ItemStack[41];
                    case "getSize" -> 41;
                    case "getHeldItemSlot" -> 0;
                    case "clear" -> null;
                    default -> throw new AssertionError(method.getName());
                });
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> id;
                    case "getInventory" -> inventory;
                    case "getItemOnCursor" -> null;
                    case "getActivePotionEffects" -> List.of();
                    case "getHealth" -> 20d;
                    case "getAbsorptionAmount" -> 0d;
                    case "getFoodLevel" -> 20;
                    case "getSaturation" -> 5f;
                    case "getExhaustion", "getExp" -> 0f;
                    case "getLevel", "getTotalExperience", "getFireTicks", "getFreezeTicks" -> 0;
                    case "getRemainingAir" -> 300;
                    case "getGameMode" -> org.bukkit.GameMode.SURVIVAL;
                    case "getLocation" -> new Location(world, 1, 70, 2);
                    case "teleport" -> true;
                    case "closeInventory", "setItemOnCursor", "setGameMode", "setHealth", "setFoodLevel", "setSaturation", "setFireTicks", "setAbsorptionAmount", "setExhaustion", "setLevel", "setExp", "setTotalExperience", "setRemainingAir", "setFreezeTicks", "kick" -> null;
                    default -> throw new AssertionError(method.getName());
                });
        GameEntryPolicy entryPolicy = new GameEntryPolicy() {
            public int priority() { return 1; }
            public Admission prepare(UUID playerId, EntryReason reason) { return new Admission(true, new Location(world, 1, 70, 2), null); }
            public void admitted(UUID playerId, EntryReason reason) {}
            public void left(UUID playerId, LeaveReason reason) {}
            public List<Location> spectatorTargets() { return List.of(); }
        };
        GameEntryService service = new GameEntryService(entryPolicy);
        service.beginRound(round, Set.of(id));
        assertEquals(EntryResult.ENTERED, service.enter(player, round, EntryReason.ROUND_START));
        service.onQuit(player);
        assertFalse(service.isPlaying(id));
        assertEquals(1, service.snapshot().savedStates().size());
    }
    @Test void shutdownCapturesWithoutForfeiting() throws Exception {
        UUID id = UUID.randomUUID();
        UUID round = UUID.randomUUID();
        World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getKey")) return NamespacedKey.minecraft("overworld");
                    throw new AssertionError(method.getName());
                });
        PlayerInventory inventory = (PlayerInventory) Proxy.newProxyInstance(PlayerInventory.class.getClassLoader(), new Class<?>[]{PlayerInventory.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getContents" -> new org.bukkit.inventory.ItemStack[41];
                    case "getSize" -> 41;
                    case "getHeldItemSlot" -> 0;
                    case "clear" -> null;
                    default -> throw new AssertionError(method.getName());
                });
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> id;
                    case "getInventory" -> inventory;
                    case "getItemOnCursor" -> null;
                    case "getActivePotionEffects" -> List.of();
                    case "getHealth" -> 20d;
                    case "getAbsorptionAmount" -> 0d;
                    case "getFoodLevel" -> 20;
                    case "getSaturation" -> 5f;
                    case "getExhaustion", "getExp" -> 0f;
                    case "getLevel", "getTotalExperience", "getFireTicks", "getFreezeTicks" -> 0;
                    case "getRemainingAir" -> 300;
                    case "getGameMode" -> org.bukkit.GameMode.SURVIVAL;
                    case "getLocation" -> new Location(world, 1, 70, 2);
                    case "teleport" -> true;
                    case "closeInventory", "setItemOnCursor", "setGameMode", "setHealth", "setFoodLevel", "setSaturation", "setFireTicks", "setAbsorptionAmount", "setExhaustion", "setLevel", "setExp", "setTotalExperience", "setRemainingAir", "setFreezeTicks", "kick" -> null;
                    default -> throw new AssertionError(method.getName());
                });
        GameEntryPolicy entryPolicy = new GameEntryPolicy() {
            public int priority() { return 1; }
            public Admission prepare(UUID playerId, EntryReason reason) { return new Admission(true, new Location(world, 1, 70, 2), null); }
            public void admitted(UUID playerId, EntryReason reason) {}
            public void left(UUID playerId, LeaveReason reason) { throw new AssertionError("shutdown forfeited participant"); }
            public List<Location> spectatorTargets() { return List.of(); }
        };
        GameEntryService service = new GameEntryService(entryPolicy);
        service.beginRound(round, Set.of(id));
        assertEquals(EntryResult.ENTERED, service.enter(player, round, EntryReason.ROUND_START));
        Server server = (Server) Proxy.newProxyInstance(Server.class.getClassLoader(), new Class<?>[]{Server.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getOnlinePlayers")) return List.of(player);
                    throw new AssertionError(method.getName());
                });
        Field field = Bukkit.class.getDeclaredField("server");
        field.setAccessible(true);
        Object previous = field.get(null);
        field.set(null, server);
        try {
            service.shutdown();
            assertEquals(1, service.snapshot().savedStates().size());
            service.onQuit(player);
        } finally {
            field.set(null, previous);
        }
        assertFalse(service.isPlaying(id));
        assertEquals(1, service.snapshot().savedStates().size());
    }
    @Test void checkpointCapturesPlayingLoadout() throws Exception {
        UUID id = UUID.randomUUID();
        UUID round = UUID.randomUUID();
        World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getKey")) return NamespacedKey.minecraft("overworld");
                    throw new AssertionError(method.getName());
                });
        PlayerInventory inventory = (PlayerInventory) Proxy.newProxyInstance(PlayerInventory.class.getClassLoader(), new Class<?>[]{PlayerInventory.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getContents" -> new org.bukkit.inventory.ItemStack[41];
                    case "getSize" -> 41;
                    case "getHeldItemSlot" -> 0;
                    case "clear" -> null;
                    default -> throw new AssertionError(method.getName());
                });
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> id;
                    case "getInventory" -> inventory;
                    case "getItemOnCursor" -> null;
                    case "getActivePotionEffects" -> List.of();
                    case "getHealth" -> 20d;
                    case "getAbsorptionAmount" -> 0d;
                    case "getFoodLevel" -> 20;
                    case "getSaturation" -> 5f;
                    case "getExhaustion", "getExp" -> 0f;
                    case "getLevel", "getTotalExperience", "getFireTicks", "getFreezeTicks" -> 0;
                    case "getRemainingAir" -> 300;
                    case "getGameMode" -> org.bukkit.GameMode.SURVIVAL;
                    case "getLocation" -> new Location(world, 1, 70, 2);
                    case "teleport" -> true;
                    case "closeInventory", "setItemOnCursor", "setGameMode", "setHealth", "setFoodLevel", "setSaturation", "setFireTicks", "setAbsorptionAmount", "setExhaustion", "setLevel", "setExp", "setTotalExperience", "setRemainingAir", "setFreezeTicks", "kick" -> null;
                    default -> throw new AssertionError(method.getName());
                });
        GameEntryPolicy entryPolicy = new GameEntryPolicy() {
            public int priority() { return 1; }
            public Admission prepare(UUID playerId, EntryReason reason) { return new Admission(true, new Location(world, 1, 70, 2), null); }
            public void admitted(UUID playerId, EntryReason reason) {}
            public void left(UUID playerId, LeaveReason reason) { throw new AssertionError("shutdown forfeited participant"); }
            public List<Location> spectatorTargets() { return List.of(); }
        };
        GameEntryService service = new GameEntryService(entryPolicy);
        service.beginRound(round, Set.of(id));
        assertEquals(EntryResult.ENTERED, service.enter(player, round, EntryReason.ROUND_START));
        Server server = (Server) Proxy.newProxyInstance(Server.class.getClassLoader(), new Class<?>[]{Server.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getOnlinePlayers")) return List.of(player);
                    throw new AssertionError(method.getName());
                });
        Field field = Bukkit.class.getDeclaredField("server");
        field.setAccessible(true);
        Object previous = field.get(null);
        field.set(null, server);
        try {
            service.capturePlaying();
            assertEquals(1, service.snapshot().savedStates().size());

        } finally {
            field.set(null, previous);
        }
        assertTrue(service.isPlaying(id));
        assertEquals(1, service.snapshot().savedStates().size());
    }
    @Test void leavingForLimboSavesBeforeLobbyTransition() {
        UUID id = UUID.randomUUID();
        UUID round = UUID.randomUUID();
        World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getKey")) return NamespacedKey.minecraft("overworld");
                    throw new AssertionError(method.getName());
                });
        PlayerInventory inventory = (PlayerInventory) Proxy.newProxyInstance(PlayerInventory.class.getClassLoader(), new Class<?>[]{PlayerInventory.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getContents" -> new org.bukkit.inventory.ItemStack[41];
                    case "getSize" -> 41;
                    case "getHeldItemSlot" -> 0;
                    case "clear" -> null;
                    default -> throw new AssertionError(method.getName());
                });
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> id;
                    case "getInventory" -> inventory;
                    case "getItemOnCursor" -> null;
                    case "getActivePotionEffects" -> List.of();
                    case "getHealth" -> 20d;
                    case "getAbsorptionAmount" -> 0d;
                    case "getFoodLevel" -> 20;
                    case "getSaturation" -> 5f;
                    case "getExhaustion", "getExp" -> 0f;
                    case "getLevel", "getTotalExperience", "getFireTicks", "getFreezeTicks" -> 0;
                    case "getRemainingAir" -> 300;
                    case "getGameMode" -> org.bukkit.GameMode.SURVIVAL;
                    case "getLocation" -> new Location(world, 1, 70, 2);
                    case "teleport" -> true;
                    case "closeInventory", "setItemOnCursor", "setGameMode", "setHealth", "setFoodLevel", "setSaturation", "setFireTicks", "setAbsorptionAmount", "setExhaustion", "setLevel", "setExp", "setTotalExperience", "setRemainingAir", "setFreezeTicks", "kick" -> null;
                    default -> throw new AssertionError(method.getName());
                });
        GameEntryPolicy entryPolicy = new GameEntryPolicy() {
            public int priority() { return 1; }
            public Admission prepare(UUID playerId, EntryReason reason) { return new Admission(true, new Location(world, 1, 70, 2), null); }
            public void admitted(UUID playerId, EntryReason reason) {}
            public void left(UUID playerId, LeaveReason reason) {}
            public List<Location> spectatorTargets() { return List.of(); }
        };
        AtomicReference<GameEntryService> reference = new AtomicReference<>();
        AtomicInteger lobbyTransitions = new AtomicInteger();
        GameEntryService service = new GameEntryService(entryPolicy, ignored -> {
            assertEquals(1, reference.get().snapshot().savedStates().size());
            lobbyTransitions.incrementAndGet();
        });
        reference.set(service);
        service.beginRound(round, Set.of(id));
        assertEquals(EntryResult.ENTERED, service.enter(player, round, EntryReason.ROUND_START));
        service.enterLimbo(player, LeaveReason.LIMBO);
        assertFalse(service.isPlaying(id));
        assertEquals(1, service.snapshot().savedStates().size());
        assertEquals(1, lobbyTransitions.get());
    }
    @Test void loginNeverAdmitsAutomatically() {
        UUID id = UUID.randomUUID();
        UUID round = UUID.randomUUID();
        GameEntryPolicy entryPolicy = new GameEntryPolicy() {
            public int priority() { return 1; }
            public Admission prepare(UUID playerId, EntryReason reason) { return new Admission(true, new Location(null, 0, 1, 0), null); }
            public void admitted(UUID playerId, EntryReason reason) {}
            public void left(UUID playerId, LeaveReason reason) {}
            public List<Location> spectatorTargets() { return List.of(); }
        };
        AtomicInteger lobby = new AtomicInteger();
        GameEntryService service = new GameEntryService(entryPolicy, ignored -> lobby.incrementAndGet());
        service.restore(new GameEntryService.Snapshot(1, round, Set.of(id), Map.of(id, new SavedGamePlayerState())));
        Player player = roundStartPlayer(id);
        assertEquals(EntryResult.ENTERED, service.enter(player, round, EntryReason.ROUND_START));
        service.onLogin(player);
        assertFalse(service.isPlaying(id));
        assertEquals(1, lobby.get());
    }
    @Test void eliminatedPlayerCanViewWithoutChangingEligibility() {
        UUID id = UUID.randomUUID();
        UUID round = UUID.randomUUID();
        Location target = new Location(null, 2, 3, 4);
        GameEntryPolicy entryPolicy = new GameEntryPolicy() {
            public int priority() { return 1; }
            public Admission prepare(UUID playerId, EntryReason reason) { return new Admission(true, target, null); }
            public void admitted(UUID playerId, EntryReason reason) {}
            public void left(UUID playerId, LeaveReason reason) {}
            public List<Location> spectatorTargets() { return List.of(target); }
        };
        PlayerInventory inventory = (PlayerInventory) Proxy.newProxyInstance(PlayerInventory.class.getClassLoader(), new Class<?>[]{PlayerInventory.class},
                (proxy, method, args) -> null);
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> id;
                    case "getInventory" -> inventory;
                    case "teleport" -> true;
                    case "closeInventory", "setItemOnCursor", "setGameMode" -> null;
                    default -> throw new AssertionError(method.getName());
                });
        GameEntryService service = new GameEntryService(entryPolicy);
        service.beginRound(round, Set.of(id));
        boolean started = ChallengeManager.isGameStarted();
        ChallengeManager.markGameStarted();
        SpectatorManager.restore(id, SpectatorReason.ELIMINATED);
        try {
            assertEquals(EntryResult.ENTERED, service.enter(player, round, EntryReason.SPECTATE));
            assertFalse(service.isPlaying(id));
            assertTrue(service.isViewing(id));
            assertEquals(SpectatorReason.ELIMINATED, SpectatorManager.getReason(id));
        } finally {
            SpectatorManager.remove(id);
            if (!started) ChallengeManager.resetGameState();
        }
    }
    @Test void genericRoundStartAdmitsParticipant() throws Exception {
        UUID id = UUID.randomUUID();
        UUID round = UUID.randomUUID();
        Location spawn = new Location(null, 1, 70, 1);
        World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getSpawnLocation")) return spawn;
                    throw new AssertionError(method.getName());
                });
        Server server = (Server) Proxy.newProxyInstance(Server.class.getClassLoader(), new Class<?>[]{Server.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getWorld")) return world;
                    throw new AssertionError(method.getName());
                });
        Player player = roundStartPlayer(id);
        GameEntryService service = new GameEntryService();
        service.beginRound(round, Set.of(id));
        Field field = Bukkit.class.getDeclaredField("server");
        field.setAccessible(true);
        Object previous = field.get(null);
        field.set(null, server);
        try {
            assertEquals(EntryResult.ENTERED, service.enter(player, round, EntryReason.ROUND_START));
        } finally {
            field.set(null, previous);
        }
    }
    @Test void preparingRoundDoesNotStartTimer() {
        UUID round = UUID.randomUUID();
        GameEntryPolicy entryPolicy = new GameEntryPolicy() {
            public int priority() { return 1; }
            public boolean preparing() { return true; }
            public Admission prepare(UUID playerId, EntryReason reason) { throw new AssertionError(); }
            public void admitted(UUID playerId, EntryReason reason) { throw new AssertionError(); }
            public void left(UUID playerId, LeaveReason reason) {}
            public List<Location> spectatorTargets() { return List.of(); }
        };
        GameEntryService service = new GameEntryService(entryPolicy);
        service.beginRound(round, Set.of(UUID.randomUUID()));
        GameTimer timer = TimerRegistry.getTimer();
        GameTimer.Mode previousMode = timer.getMode();
        try {
            timer.setMode(GameTimer.Mode.COUNT_UP);
            timer.pause();
            service.completeRoundStart();
            service.completeRoundStart();
            assertFalse(timer.isRunning());
            assertEquals(round, service.currentRoundId());
        } finally {
            timer.setMode(previousMode);
        }
    }
    @Test void queuedAdmissionIsRestoredOnResume() throws Exception {
        UUID id = UUID.randomUUID();
        UUID round = UUID.randomUUID();
        SavedGamePlayerState saved = new SavedGamePlayerState();
        saved.inventory = new ArrayList<>(Collections.nCopies(41, null));
        saved.cursor = new ArrayList<>(Collections.singletonList(null));
        saved.effects = List.of();
        saved.world = "minecraft:overworld";
        saved.gameMode = "SURVIVAL";
        saved.health = 20;
        AtomicInteger restored = new AtomicInteger();
        PlayerInventory inventory = (PlayerInventory) Proxy.newProxyInstance(PlayerInventory.class.getClassLoader(), new Class<?>[]{PlayerInventory.class},
                (proxy, method, args) -> { if (method.getName().equals("setContents")) restored.incrementAndGet(); if (method.getName().equals("getContents")) return new org.bukkit.inventory.ItemStack[41]; if (method.getName().equals("getSize")) return 41; if (method.getName().equals("getHeldItemSlot")) return 0; return null; });
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> id;
                    case "getInventory" -> inventory;
                    case "getActivePotionEffects" -> List.of();
                    case "getLocation" -> new Location(Bukkit.getWorld(NamespacedKey.minecraft("overworld")), 0, 70, 0);
                    case "getGameMode" -> org.bukkit.GameMode.ADVENTURE;
                    case "getHealth" -> 20d;
                    case "getAbsorptionAmount" -> 0d;
                    case "getSaturation", "getExhaustion", "getExp" -> 0f;
                    case "getFoodLevel", "getLevel", "getTotalExperience", "getFireTicks", "getFreezeTicks", "getRemainingAir" -> 0;
                    case "teleport" -> true;
                    default -> null;
                });
        World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> { if (method.getName().equals("getKey")) return NamespacedKey.minecraft("overworld"); throw new AssertionError(method.getName()); });
        Server server = (Server) Proxy.newProxyInstance(Server.class.getClassLoader(), new Class<?>[]{Server.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getPlayer" -> player;
                    case "getWorld" -> world;
                    default -> throw new AssertionError(method.getName());
                });
        GameEntryService service = new GameEntryService();
        service.restore(new GameEntryService.Snapshot(1, round, Set.of(id), Map.of(id, saved)));
        Field field = Bukkit.class.getDeclaredField("server");
        field.setAccessible(true);
        Object previous = field.get(null);
        GameTimer timer = TimerRegistry.getTimer();
        GameTimer.Mode previousMode = timer.getMode();
        field.set(null, server);
        try {
            timer.setMode(GameTimer.Mode.COUNT_UP);
            timer.pause();
            assertEquals(EntryResult.QUEUED, service.enter(player, round, EntryReason.REJOIN));
            timer.start();
            service.resumePending();
            service.resumePending();
            assertEquals(1, restored.get());
        } finally {
            timer.pause();
            timer.setMode(previousMode);
            field.set(null, previous);
        }
    }
    @Test void pendingAdmissionSurvivesSnapshot() {
        UUID id = UUID.randomUUID();
        UUID round = UUID.randomUUID();
        GameEntryService service = new GameEntryService();
        service.restore(new GameEntryService.Snapshot(1, round, Set.of(id), Map.of(id, new SavedGamePlayerState())));
        GameTimer timer = TimerRegistry.getTimer();
        GameTimer.Mode previousMode = timer.getMode();
        try {
            timer.setMode(GameTimer.Mode.COUNT_UP);
            timer.pause();
            assertEquals(EntryResult.QUEUED, service.enter(player(id), round, EntryReason.REJOIN));
            GameEntryService.Snapshot snapshot = new Gson().fromJson(new Gson().toJson(service.snapshot()), GameEntryService.Snapshot.class);
            GameEntryService restored = new GameEntryService(null, ignored -> {});
            restored.restore(snapshot);
            restored.onLogin(player(id));
            assertEquals(EntryReason.REJOIN, restored.snapshot().pending().get(id));
        } finally {
            timer.setMode(previousMode);
        }
    }
    @Test void failedPolicyHookDoesNotCommitPlayingPresence() {
        UUID id = UUID.randomUUID();
        UUID round = UUID.randomUUID();
        GameEntryPolicy entryPolicy = new GameEntryPolicy() {
            public int priority() { return 1; }
            public Admission prepare(UUID playerId, EntryReason reason) { return new Admission(true, new Location(null, 1, 2, 3), null); }
            public void admitted(UUID playerId, EntryReason reason) { throw new IllegalStateException("admission failed"); }
            public void left(UUID playerId, LeaveReason reason) {}
            public List<Location> spectatorTargets() { return List.of(); }
        };
        GameEntryService service = new GameEntryService(entryPolicy);
        service.beginRound(round, Set.of(id));
        Player player = roundStartPlayer(id);
        assertEquals(EntryResult.DENIED, service.enter(player, round, EntryReason.ROUND_START));
        assertFalse(service.isPlaying(id));
    }
    @Test void savedLobbyVisitorStateSurvivesRestartWithoutRound() {
        UUID id = UUID.randomUUID();
        SavedGamePlayerState saved = new SavedGamePlayerState();
        GameEntryService service = new GameEntryService();
        service.restore(new GameEntryService.Snapshot(1, null, Set.of(), Map.of(id, saved)));
        assertEquals(1, service.snapshot().savedStates().size());
    }
    @Test void roundStartSavesTemporaryInventoryBeforeClearingIt() {
        UUID id = UUID.randomUUID();
        UUID round = UUID.randomUUID();
        AtomicInteger captured = new AtomicInteger();
        AtomicInteger cleared = new AtomicInteger();
        World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getKey")) return NamespacedKey.minecraft("overworld");
                    throw new AssertionError(method.getName());
                });
        PlayerInventory inventory = (PlayerInventory) Proxy.newProxyInstance(PlayerInventory.class.getClassLoader(), new Class<?>[]{PlayerInventory.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getContents" -> { captured.incrementAndGet(); yield new org.bukkit.inventory.ItemStack[41]; }
                    case "getSize" -> 41;
                    case "getHeldItemSlot" -> 0;
                    case "clear" -> { assertEquals(1, captured.get()); cleared.incrementAndGet(); yield null; }
                    default -> throw new AssertionError(method.getName());
                });
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> id;
                    case "getInventory" -> inventory;
                    case "getItemOnCursor" -> null;
                    case "getActivePotionEffects" -> List.of();
                    case "getHealth" -> 20d;
                    case "getAbsorptionAmount" -> 0d;
                    case "getFoodLevel" -> 20;
                    case "getSaturation" -> 5f;
                    case "getExhaustion", "getExp" -> 0f;
                    case "getLevel", "getTotalExperience", "getFireTicks", "getFreezeTicks" -> 0;
                    case "getRemainingAir" -> 300;
                    case "getGameMode" -> org.bukkit.GameMode.ADVENTURE;
                    case "getLocation" -> new Location(world, 0, 190, 0);
                    case "teleport" -> true;
                    case "closeInventory", "setItemOnCursor", "setGameMode", "setHealth", "setFoodLevel", "setSaturation", "setFireTicks", "setAbsorptionAmount", "setExhaustion", "setLevel", "setExp", "setTotalExperience", "setRemainingAir", "setFreezeTicks", "kick" -> null;
                    default -> throw new AssertionError(method.getName());
                });
        GameEntryPolicy entryPolicy = new GameEntryPolicy() {
            public int priority() { return 1; }
            public Admission prepare(UUID playerId, EntryReason reason) { return new Admission(true, new Location(world, 0, 70, 0), null); }
            public void admitted(UUID playerId, EntryReason reason) {}
            public void left(UUID playerId, LeaveReason reason) {}
            public List<Location> spectatorTargets() { return List.of(); }
        };
        GameEntryService service = new GameEntryService(entryPolicy);
        service.beginRound(round, Set.of(id));
        assertEquals(EntryResult.ENTERED, service.enter(player, round, EntryReason.ROUND_START));
        assertEquals(1, cleared.get());
    }
    @Test void resetDuringAdmissionCannotRecreateSavedState() {
        UUID id = UUID.randomUUID();
        UUID round = UUID.randomUUID();
        AtomicReference<GameEntryService> reference = new AtomicReference<>();
        GameEntryPolicy policy = new GameEntryPolicy() {
            public int priority() { return 1; }
            public Admission prepare(UUID playerId, EntryReason reason) { return new Admission(true, new Location(null, 0, 70, 0), null); }
            public void admitted(UUID playerId, EntryReason reason) { reference.get().reset(); }
            public void left(UUID playerId, LeaveReason reason) {}
            public List<Location> spectatorTargets() { return List.of(); }
        };
        GameEntryService service = new GameEntryService(policy);
        reference.set(service);
        service.beginRound(round, Set.of(id));
        assertEquals(EntryResult.DENIED, service.enter(roundStartPlayer(id), round, EntryReason.ROUND_START));
        assertTrue(service.snapshot().savedStates().isEmpty());
    }
    @Test void combinedChallengesReceiveOneInitialAdmission() {
        UUID id = UUID.randomUUID();
        UUID round = UUID.randomUUID();
        AtomicInteger hooks = new AtomicInteger();
        Set<Challenge> previous = new HashSet<>(ChallengeManager.getActiveChallenges());
        try {
            ChallengeManager.getActiveChallenges().clear();
            ChallengeManager.getActiveChallenges().add(new AdmissionChallenge(ChallengeType.RANDOMIZER, hooks));
            ChallengeManager.getActiveChallenges().add(new AdmissionChallenge(ChallengeType.ITEM_RAIN, hooks));
            GameEntryService service = new GameEntryService();
            service.beginRound(round, Set.of(id));
            Player player = roundStartPlayer(id);
            World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("getSpawnLocation")) return new Location(null, 1, 70, 1);
                        throw new AssertionError(method.getName());
                    });
            Server server = (Server) Proxy.newProxyInstance(Server.class.getClassLoader(), new Class<?>[]{Server.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("getWorld")) return world;
                        throw new AssertionError(method.getName());
                    });
            Field field = Bukkit.class.getDeclaredField("server");
            field.setAccessible(true);
            Object old = field.get(null);
            field.set(null, server);
            try {
                assertEquals(EntryResult.ENTERED, service.enter(player, round, EntryReason.ROUND_START));
                assertEquals(EntryResult.DENIED, service.enter(player, round, EntryReason.ROUND_START));
                assertEquals(2, hooks.get());
            } finally {
                field.set(null, old);
            }
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        } finally {
            ChallengeManager.getActiveChallenges().clear();
            ChallengeManager.getActiveChallenges().addAll(previous);
        }
    }

    private record AdmissionChallenge(ChallengeType type, AtomicInteger hooks) implements Challenge {
        public ChallengeType getType() { return type; }
        public List<KatzeCommand> getCommands() { return List.of(); }
        public List<Listener> getListeners() { return List.of(); }
        public List<Runnable> getTasks() { return List.of(); }
        public void onPlayerAdmitted(Player player, EntryReason reason) { hooks.incrementAndGet(); }
    }
    @Test void playingDepartureNotifiesAllChallenges() {
        UUID id = UUID.randomUUID();
        UUID round = UUID.randomUUID();
        AtomicReference<LeaveReason> observed = new AtomicReference<>();
        Set<Challenge> previous = new HashSet<>(ChallengeManager.getActiveChallenges());
        try {
            ChallengeManager.getActiveChallenges().clear();
            ChallengeManager.getActiveChallenges().add(new LeaveChallenge(observed));
            GameEntryPolicy entryPolicy = new GameEntryPolicy() {
                public int priority() { return 1; }
                public Admission prepare(UUID playerId, EntryReason reason) { return new Admission(true, new Location(null, 0, 70, 0), null); }
                public void admitted(UUID playerId, EntryReason reason) {}
                public void left(UUID playerId, LeaveReason reason) {}
                public List<Location> spectatorTargets() { return List.of(); }
            };
            GameEntryService service = new GameEntryService(entryPolicy);
            service.beginRound(round, Set.of(id));
            Player player = roundStartPlayer(id);
            assertEquals(EntryResult.ENTERED, service.enter(player, round, EntryReason.ROUND_START));
            service.onQuit(player);
            assertEquals(LeaveReason.DISCONNECT, observed.get());
        } finally {
            ChallengeManager.getActiveChallenges().clear();
            ChallengeManager.getActiveChallenges().addAll(previous);
        }
    }

    private record LeaveChallenge(AtomicReference<LeaveReason> observed) implements Challenge {
        public ChallengeType getType() { return ChallengeType.DEATH_SWAP; }
        public List<KatzeCommand> getCommands() { return List.of(); }
        public List<Listener> getListeners() { return List.of(); }
        public List<Runnable> getTasks() { return List.of(); }
        public void onPlayerLeaving(Player player, LeaveReason reason) { observed.set(reason); }
    }
    @Test void spectatingIsDeniedBeforeTheRoundStarts() throws Exception {
        Field started = ChallengeManager.class.getDeclaredField("gameStarted");
        started.setAccessible(true);
        boolean previous = started.getBoolean(null);
        started.setBoolean(null, false);
        try {
            UUID id = UUID.randomUUID();
            UUID round = UUID.randomUUID();
            GameEntryService service = new GameEntryService(policy(1));
            service.beginRound(round, Set.of(id));
            assertEquals(EntryResult.DENIED, service.enter(player(id), round, EntryReason.SPECTATE));
        } finally {
            started.setBoolean(null, previous);
        }
    }
    @Test void waitingParticipantCannotProgress() {
        UUID id = UUID.randomUUID();
        GameEntryService service = new GameEntryService();
        service.beginRound(UUID.randomUUID(), Set.of(id));
        assertFalse(service.allowsProgress(id));
    }
    @Test void restoredRoundDoesNotRestartTimerDuringPreparationTick() throws Exception {
        UUID id = UUID.randomUUID();
        GameEntryService service = new GameEntryService(policy(1));
        service.restore(new GameEntryService.Snapshot(1, UUID.randomUUID(), Set.of(id), Map.of()));
        service.setRoutingEnabled(true);
        Field field = ChallengeManager.class.getDeclaredField("gameStarted");
        field.setAccessible(true);
        boolean oldStarted = field.getBoolean(null);
        field.setBoolean(null, true);
        GameTimer timer = TimerRegistry.getTimer();
        GameTimer.Mode oldMode = timer.getMode();
        try {
            timer.setMode(GameTimer.Mode.COUNT_UP);
            timer.pause();
            service.tick();
            assertFalse(timer.isRunning());
        } finally {
            timer.setMode(oldMode);
            field.setBoolean(null, oldStarted);
        }
    }
    @Test void preparingRoundResumesItsAdmissionBoundaryAfterRestart() throws Exception {
        AtomicBoolean preparing = new AtomicBoolean(true);
        GameEntryPolicy entryPolicy = new GameEntryPolicy() {
            public int priority() { return 1; }
            public boolean preparing() { return preparing.get(); }
            public Admission prepare(UUID id, EntryReason reason) { return new Admission(false, null, null); }
            public void admitted(UUID id, EntryReason reason) {}
            public void left(UUID id, LeaveReason reason) {}
            public List<Location> spectatorTargets() { return List.of(); }
        };
        GameEntryService original = new GameEntryService(entryPolicy);
        original.beginRound(UUID.randomUUID(), Set.of(UUID.randomUUID()));
        GameEntryService.Snapshot snapshot = new Gson().fromJson(new Gson().toJson(original.snapshot()), GameEntryService.Snapshot.class);
        GameEntryService restored = new GameEntryService(entryPolicy);
        restored.restore(snapshot);
        restored.setRoutingEnabled(true);
        Field serverField = Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        Object oldServer = serverField.get(null);
        Server server = (Server) Proxy.newProxyInstance(Server.class.getClassLoader(), new Class<?>[]{Server.class},
                (proxy, method, args) -> { if (method.getName().equals("getPlayer")) return null; throw new AssertionError(method.getName()); });
        Field startedField = ChallengeManager.class.getDeclaredField("gameStarted");
        startedField.setAccessible(true);
        boolean oldStarted = startedField.getBoolean(null);
        GameTimer timer = TimerRegistry.getTimer();
        GameTimer.Mode oldMode = timer.getMode();
        serverField.set(null, server);
        startedField.setBoolean(null, true);
        try {
            timer.setMode(GameTimer.Mode.COUNT_UP);
            timer.pause();
            restored.tick();
            assertFalse(timer.isRunning());
            preparing.set(false);
            restored.tick();
            assertFalse(timer.isRunning());
            restored.resumePreparedRound();
            assertTrue(timer.isRunning());
        } finally {
            timer.pause();
            timer.setMode(oldMode);
            startedField.setBoolean(null, oldStarted);
            serverField.set(null, oldServer);
        }
    }
    @Test void repeatedReadinessStartsTimerOnlyOnce() {
        AtomicInteger starts = new AtomicInteger();
        GameTimer timer = (GameTimer) Proxy.newProxyInstance(GameTimer.class.getClassLoader(), new Class<?>[]{GameTimer.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getMode" -> GameTimer.Mode.COUNT_UP;
                    case "start" -> { starts.incrementAndGet(); yield null; }
                    default -> throw new AssertionError(method.getName());
                });
        GameTimer old = TimerRegistry.getTimer();
        TimerRegistry.setTimer(timer);
        try {
            GameEntryService service = new GameEntryService(policy(1));
            UUID round = UUID.randomUUID();
            service.beginRound(round, Set.of());
            service.completeRoundStart();
            service.completeRoundStart();
            assertEquals(round, service.currentRoundId());
            assertEquals(1, starts.get());
        } finally {
            TimerRegistry.setTimer(old);
        }
    }
    private Player roundStartPlayer(UUID id) {
        World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getKey")) return NamespacedKey.minecraft("overworld");
                    throw new AssertionError(method.getName());
                });
        PlayerInventory inventory = (PlayerInventory) Proxy.newProxyInstance(PlayerInventory.class.getClassLoader(), new Class<?>[]{PlayerInventory.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getContents" -> new org.bukkit.inventory.ItemStack[41];
                    case "getSize" -> 41;
                    case "getHeldItemSlot" -> 0;
                    case "clear" -> null;
                    default -> throw new AssertionError(method.getName());
                });
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> id;
                    case "getInventory" -> inventory;
                    case "getItemOnCursor" -> null;
                    case "getActivePotionEffects" -> List.of();
                    case "getHealth" -> 20d;
                    case "getAbsorptionAmount" -> 0d;
                    case "getFoodLevel" -> 20;
                    case "getSaturation" -> 5f;
                    case "getExhaustion", "getExp" -> 0f;
                    case "getLevel", "getTotalExperience", "getFireTicks", "getFreezeTicks" -> 0;
                    case "getRemainingAir" -> 300;
                    case "getGameMode" -> org.bukkit.GameMode.ADVENTURE;
                    case "getLocation" -> new Location(world, 0, 190, 0);
                    case "teleport" -> true;
                    case "closeInventory", "setItemOnCursor", "setGameMode", "setHealth", "setFoodLevel", "setSaturation", "setFireTicks", "setAbsorptionAmount", "setExhaustion", "setLevel", "setExp", "setTotalExperience", "setRemainingAir", "setFreezeTicks", "kick" -> null;
                    default -> throw new AssertionError(method.getName());
                });
    }
    private GameEntryPolicy policy(int priority) {
        return new GameEntryPolicy() {
            public int priority() { return priority; }
            public Admission prepare(UUID playerId, EntryReason reason) { return new Admission(false, null, null); }
            public void admitted(UUID playerId, EntryReason reason) {}
            public void left(UUID playerId, LeaveReason reason) {}
            public List<Location> spectatorTargets() { return List.of(); }
        };
    }
    private Player player(UUID id) {
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("getUniqueId")) return id;
                    if (method.getName().equals("closeInventory")) return null;
                    throw new AssertionError("Unexpected player operation: " + method.getName());
                });
    }
}
