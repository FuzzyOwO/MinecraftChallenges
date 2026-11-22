package gg.itzkatze.katze.replay;

import gg.itzkatze.katze.KatzeGames;
import gg.itzkatze.katze.challenges.ChallengeManager;
import gg.itzkatze.katze.challenges.ChallengeRegistry;
import gg.itzkatze.katze.challenges.ChallengeType;
import gg.itzkatze.katze.game.GameWorldDirectory;
import gg.itzkatze.katze.lobby.LobbyManager;
import gg.itzkatze.katze.results.BattleResultsStore;
import gg.itzkatze.katze.results.SavedBattle;
import gg.itzkatze.katze.scoreboard.ScoreboardManager;
import gg.itzkatze.katze.util.JsonFiles;
import gg.itzkatze.katze.text.TextArg;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.function.Supplier;

public final class ReplayService {
    private static volatile ReplayService instance;
    private final KatzeGames plugin;
    private final ReplayStore store;
    private final ExecutorService writer = Executors.newSingleThreadExecutor(Thread.ofPlatform().name("KatzeGames-replay-writer").factory());
    private final ExecutorService reader = Executors.newFixedThreadPool(2, Thread.ofPlatform().name("KatzeGames-replay-reader-", 0).factory());
    private final Map<UUID, ReplayRecorder> recordings = new HashMap<>();
    private final Map<UUID, ReplaySession> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> loading = new ConcurrentHashMap<>();
    private final Set<ReplaySession> pendingClose = new HashSet<>();
    private final Set<UUID> swings = new HashSet<>();
    private final Set<UUID> hurt = new HashSet<>();
    private final ReplayMining mining = new ReplayMining();
    private final BukkitTask task;
    private record Review(ReplayStore.Loaded replay, SavedBattle battle) {}

    record ReturnState(String world, double x, double y, double z, float yaw, float pitch, String mode, boolean flight, boolean flying,
                       List<String> inventory, int heldSlot, String cursor, boolean invisible, boolean invulnerable, boolean collidable) {
        static ReturnState capture(Player player) {
            Location at = player.getLocation();
            return new ReturnState(at.getWorld().getKey().asString(), at.getX(), at.getY(), at.getZ(), at.getYaw(), at.getPitch(),
                    player.getGameMode().name(), player.getAllowFlight(), player.isFlying(), ReplayItems.encode(player.getInventory().getContents()),
                    player.getInventory().getHeldItemSlot(), ReplayItems.encode(player.getItemOnCursor()), player.isInvisible(), player.isInvulnerable(), player.isCollidable());
        }

        boolean restore(Player player) {
            World target = Bukkit.getWorld(NamespacedKey.fromString(world));
            Location location = target == null || isReplayWorld(target) ? LobbyManager.getSpawn() : new Location(target, x, y, z, yaw, pitch);
            if (location == null) return false;
            if (player.getGameMode() == GameMode.SPECTATOR) player.setSpectatorTarget(null);
            if (!player.teleport(location)) return false;
            apply(player);
            return true;
        }

        Location restoreInLimbo(Player player) {
            World target = Bukkit.getWorld(NamespacedKey.fromString(world));
            if (target == null || isReplayWorld(target)) throw new IllegalStateException("Replay return world is unavailable: " + world);
            Location location = new Location(target, x, y, z, yaw, pitch);
            if (player.getGameMode() == GameMode.SPECTATOR) player.setSpectatorTarget(null);
            apply(player);
            return location;
        }

        private void apply(Player player) {
            ItemStack[] contents = inventory == null ? null : ReplayItems.decode(inventory);
            ItemStack cursorItem = inventory == null ? null : ReplayItems.decode(cursor);
            GameMode gameMode = GameMode.valueOf(mode);
            player.setGameMode(gameMode);
            player.setAllowFlight(flight);
            player.setFlying(flight && flying);
            if (contents != null) {
                player.getInventory().setContents(contents);
                player.getInventory().setHeldItemSlot(Math.clamp(heldSlot, 0, 8));
                player.setItemOnCursor(cursorItem);
                player.setInvisible(invisible);
                player.setInvulnerable(invulnerable);
                player.setCollidable(collidable);
            }
        }
    }

    private ReplayService(KatzeGames plugin) {
        this.plugin = plugin;
        store = new ReplayStore(plugin.getDataDirectory().toPath().resolve("data/replays"));
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1, 1);
    }

    public static void install(KatzeGames plugin) {
        instance = new ReplayService(plugin);
        Bukkit.getPluginManager().registerEvents(new ReplayListener(), plugin);
    }

    public static ReplayService get() { return instance; }

    <T> CompletableFuture<T> read(Supplier<T> operation) { return CompletableFuture.supplyAsync(operation, reader); }

    public static void markCombat(UUID round) {
        ReplayService service = instance;
        if (service == null) return;
        ReplayRecorder recorder = service.recordings.get(round);
        if (recorder != null) recorder.markCombat();
    }

    public void recoverWorlds() {
        Bukkit.getOnlinePlayers().forEach(this::recover);
        Path dimensions = Bukkit.getServer().getLevelDirectory().resolve("dimensions/katzegames");
        if (!Files.isDirectory(dimensions)) return;
        try (var paths = Files.list(dimensions)) {
            for (Path path : paths.filter(Files::isDirectory).filter(path -> path.getFileName().toString().matches("replay_[0-9a-f-]{36}_[0-9a-f-]{36}")).toList()) {
                NamespacedKey key = new NamespacedKey("katzegames", path.getFileName().toString());
                World world = Bukkit.getWorld(key);
                GameWorldDirectory directory = new GameWorldDirectory(Bukkit.getServer(), key);
                if (world != null) {
                    directory.validateWorldPath(world.getWorldPath());
                    if (!world.getPlayers().isEmpty() || !Bukkit.unloadWorld(world, false)) continue;
                }
                directory.delete();
            }
        } catch (IOException e) { log("Failed to clean interrupted replay worlds", e); }
    }

    public static void beginRound(UUID round, List<UUID> participants, ChallengeType type) {
        beginRound(round, participants, type, true);
    }

    public static void beginRound(UUID round, List<UUID> participants, ChallengeType type, boolean battleStarted) {
        if (instance == null || participants.isEmpty() || !instance.plugin.getConfig().getBoolean("replays.enabled", true)) return;
        if (!ReplaySaveMode.from(ChallengeRegistry.getChallenge(type)).records(battleStarted)) return;
        if (instance.recordings.containsKey(round)) return;
        try {
            ReplayRecorder recorder = new ReplayRecorder(round, participants, instance.store, instance.writer);
            instance.recordings.put(round, recorder);
            recorder.tick(Set.of());
        }
        catch (IOException e) { instance.log("Failed to start replay recording " + round, e); }
    }

    public static void finishRound(UUID round) {
        if (instance == null) return;
        ReplayRecorder recorder = instance.recordings.remove(round);
        if (recorder == null) return;
        ReplayService service = instance;
        recorder.capture(Set.copyOf(service.swings), Set.copyOf(service.hurt), service.mining.snapshot(), true);
        recorder.finish().exceptionally(error -> { service.log("Failed to save replay " + round, error); return null; });
    }

    public static void checkpoint(UUID round) {
        ReplayService service = instance;
        if (service == null) return;
        ReplayRecorder recorder = service.recordings.get(round);
        if (recorder != null) recorder.capture(Set.copyOf(service.swings), Set.copyOf(service.hurt), service.mining.snapshot(), true);
    }

    public boolean available(UUID round) { return store.available(round); }
    public boolean reviewing(UUID player) { return sessions.containsKey(player) || loading.containsKey(player); }
    public ReplaySession session(UUID player) { return sessions.get(player); }
    public List<String> joinablePlayers(UUID viewer) {
        return sessions.entrySet().stream().filter(entry -> !entry.getKey().equals(viewer) && entry.getValue().canJoin())
                .map(entry -> Bukkit.getPlayer(entry.getKey())).filter(player -> player != null && player.isOnline())
                .map(Player::getName).sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }
    void swing(UUID player) { swings.add(player); }
    void hurt(UUID entity) { hurt.add(entity); }
    void startMining(Player player, Block block) { if (!recordings.isEmpty() && !isReplayWorld(block.getWorld())) mining.start(player, block); }
    void stopMining(UUID player) { mining.stop(player); }
    void changed(Block block) { recordings.values().forEach(recording -> recording.changed(block)); }
    void interactContainer(Player player, Block block) {
        if (!isReplayWorld(block.getWorld())) recordings.values().forEach(recording -> recording.interactContainer(player, block));
    }

    public static boolean isReplayWorld(World world) {
        return world.getKey().getNamespace().equals("katzegames") && world.getKey().getKey().startsWith("replay_");
    }

    public void play(Player player, UUID round) {
        if (reviewing(player.getUniqueId())) return;
        if (ChallengeManager.isGameStarted()) {
            player.sendMessage(Component.translatable("katze.replay.lobby_only"));
            return;
        }
        if (sessions.size() + loading.size() >= Math.max(1, plugin.getConfig().getInt("replays.max-viewers", 4))) {
            player.sendMessage(Component.translatable("katze.replay.busy"));
            return;
        }
        UUID token = UUID.randomUUID();
        UUID viewer = player.getUniqueId();
        loading.put(viewer, token);
        player.closeInventory();
        player.sendMessage(Component.translatable("katze.replay.loading"));
        String minecraft = Bukkit.getMinecraftVersion();
        CompletableFuture.supplyAsync(() -> {
            try {
                SavedBattle battle = BattleResultsStore.installed().findForPlayer(round, viewer).orElseThrow(() -> new IOException("Not a participant"));
                ReplayStore.Loaded loaded = store.load(round, viewer, minecraft);
                if (battle.game() == SavedBattle.Game.MOB_ARMY) loaded = store.withCombatCheckpoint(loaded);
                return new Review(loaded, battle);
            } catch (Exception e) { throw new IllegalStateException(e); }
        }, reader).whenComplete((replay, error) -> {
            if (!plugin.isEnabled()) return;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!loading.remove(viewer, token) || !player.isOnline() || ChallengeManager.isGameStarted()) return;
                if (error != null) {
                    log("Failed to load replay " + round, error);
                    player.sendMessage(Component.translatable("katze.replay.unavailable"));
                    return;
                }
                ReplaySession session = null;
                try {
                    JsonFiles.write(returnFile(viewer), ReturnState.capture(player));
                    session = new ReplaySession(player, replay.replay(), replay.battle());
                    attach(player, session);
                    player.sendMessage(Component.translatable("katze.replay.started"));
                    player.sendMessage(Component.translatable("katze.replay.sharing_hint", TextArg.text("player", player.getName())));
                } catch (Exception e) {
                    log("Failed to start replay " + round, e);
                    leave(player);
                    player.sendMessage(Component.translatable("katze.replay.unavailable"));
                }
            });
        });
    }

    public void join(Player player, String name) {
        if (reviewing(player.getUniqueId())) {
            player.sendMessage(Component.translatable("katze.replay.already_reviewing"));
            return;
        }
        if (ChallengeManager.isGameStarted()) {
            player.sendMessage(Component.translatable("katze.replay.lobby_only"));
            return;
        }
        Player target = Bukkit.getPlayerExact(name);
        ReplaySession existing = target == null ? null : sessions.get(target.getUniqueId());
        if (existing == null || !existing.canJoin()) {
            player.sendMessage(Component.translatable("katze.replay.no_active_session", TextArg.text("player", name)));
            return;
        }
        player.closeInventory();
        try {
            JsonFiles.write(returnFile(player.getUniqueId()), ReturnState.capture(player));
            ReplaySession session = new ReplaySession(player, existing);
            attach(player, session);
            player.sendMessage(Component.translatable("katze.replay.joined", TextArg.text("player", target.getName())));
        } catch (Exception error) {
            log("Failed to join shared replay", error);
            leave(player);
            player.sendMessage(Component.translatable("katze.replay.unavailable"));
        }
    }

    private void attach(Player player, ReplaySession session) {
        sessions.put(player.getUniqueId(), session);
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setInvulnerable(true);
        player.setInvisible(true);
        player.setCollidable(false);
        session.start();
    }

    private void tick() {
        mining.tick();
        Set<UUID> animations = Set.copyOf(swings);
        Set<UUID> injuries = Set.copyOf(hurt);
        List<ReplayFrame.Mining> breaking = mining.snapshot();
        for (ReplayRecorder recording : recordings.values()) {
            try { recording.capture(animations, injuries, breaking, false); } catch (RuntimeException e) { log("Failed to capture replay frame", e); }
        }
        swings.clear();
        hurt.clear();
        for (var entry : new ArrayList<>(sessions.entrySet())) {
            if (sessions.get(entry.getKey()) != entry.getValue()) continue;
            try { entry.getValue().tick(); }
            catch (RuntimeException e) {
                log("Failed to play replay", e);
                for (var member : new ArrayList<>(sessions.entrySet())) if (member.getValue().sharesPlayback(entry.getValue())) {
                    Player player = Bukkit.getPlayer(member.getKey());
                    if (player != null) leave(player);
                }
            }
        }
    }

    public void leave(Player player) {
        loading.remove(player.getUniqueId());
        ReplaySession session = sessions.remove(player.getUniqueId());
        player.closeInventory();
        if (!recover(player) && session != null) {
            sessions.put(player.getUniqueId(), session);
            player.sendMessage(Component.translatable("katze.replay.return_failed"));
            return;
        }
        if (session != null) {
            try { session.close(); } catch (IOException e) { log("Failed to clean replay worlds", e); }
        }
        player.sendActionBar(Component.empty());
        ScoreboardManager.updateAll();
    }

    public record LimboRecovery(boolean success, Location location, boolean pendingFile) {}

    public LimboRecovery recoverForLimbo(Player player) {
        try {
            ReturnState state = JsonFiles.read(returnFile(player.getUniqueId()), ReturnState.class);
            if (state == null) return new LimboRecovery(true, null, false);
            return new LimboRecovery(true, state.restoreInLimbo(player), true);
        } catch (IOException | RuntimeException e) {
            log("Failed to restore replay viewer " + player.getUniqueId(), e);
            return new LimboRecovery(false, null, true);
        }
    }

    public void finishLimboRecovery(Player player) {
        try {
            Files.deleteIfExists(returnFile(player.getUniqueId()));
        } catch (IOException e) {
            log("Failed to remove recovered replay state for " + player.getUniqueId(), e);
        }
    }
    public boolean recover(Player player) {
        try {
            ReturnState state = JsonFiles.read(returnFile(player.getUniqueId()), ReturnState.class);
            if (state == null) return true;
            if (!state.restore(player)) return false;
            Files.deleteIfExists(returnFile(player.getUniqueId()));
            return true;
        } catch (IOException | RuntimeException e) { log("Failed to restore replay viewer " + player.getUniqueId(), e); return false; }
    }

    public void disconnect(Player player) {
        loading.remove(player.getUniqueId());
        ReplaySession session = sessions.remove(player.getUniqueId());
        if (session == null) return;
        recover(player);
        pendingClose.add(session);
        Bukkit.getScheduler().runTask(plugin, () -> closeDisconnected(session));
    }

    private void closeDisconnected(ReplaySession session) {
        if (!pendingClose.remove(session)) return;
        try { session.close(); }
        catch (IOException e) { log("Failed to clean disconnected replay viewer", e); }
    }

    public void closeViewers() {
        loading.clear();
        for (UUID id : new ArrayList<>(sessions.keySet())) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) leave(player);
        }
    }

    public void finishRecordings() { new ArrayList<>(recordings.keySet()).forEach(ReplayService::finishRound); }

    public void shutdown() {
        task.cancel();
        closeViewers();
        new ArrayList<>(pendingClose).forEach(this::closeDisconnected);
        finishRecordings();
        reader.shutdownNow();
        writer.shutdown();
        try {
            if (!writer.awaitTermination(30, TimeUnit.SECONDS)) log("Replay writer did not finish before shutdown", new IOException("Timed out"));
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        instance = null;
    }

    private Path returnFile(UUID player) { return plugin.getDataDirectory().toPath().resolve("data/replay-returns").resolve(player + ".json"); }
    private void log(String message, Throwable error) { plugin.getLogger().log(Level.WARNING, message, error); }
}
