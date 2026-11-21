package gg.itzkatze.katze.game.entry;

import gg.itzkatze.katze.KatzeGames;
import gg.itzkatze.katze.game.GameTimer;
import gg.itzkatze.katze.game.GameLifecycleManager;
import gg.itzkatze.katze.game.SessionPersistence;
import gg.itzkatze.katze.player.KatzePlayer;
import java.util.function.Consumer;
import gg.itzkatze.katze.spectator.SpectatorManager;
import gg.itzkatze.katze.challenges.Challenge;
import gg.itzkatze.katze.challenges.ChallengeManager;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import gg.itzkatze.katze.game.GameWorldManager;
import org.bukkit.Location;

public final class GameEntryService {
    private static final GameEntryService INSTANCE = new GameEntryService();
    private final GameEntryPolicy suppliedPolicy;
    private Consumer<Player> lobbyTransition;
    private GameEntryPolicy policy;
    private UUID roundId;
    private Set<UUID> participants = Set.of();
    private GamePresence presence;
    private boolean shuttingDown;
    private boolean routingEnabled;
    private boolean roundStarted;
    private boolean restoredPreparationPaused;
    private boolean startingAdmissions;
    private final Set<UUID> initialAdmitted = new HashSet<>();
    private final Map<UUID, SavedGamePlayerState> savedStates = new HashMap<>();
    private final Map<UUID, PendingAdmission> pending = new HashMap<>();
    private final Map<UUID, Long> callbackGenerations = new HashMap<>();
    private record PendingAdmission(UUID roundId, long generation, EntryReason reason) {}

    public GameEntryService() { this(null); }

    GameEntryService(GameEntryPolicy policy) {
        this(policy, player -> GameLifecycleManager.sendToLobby(KatzePlayer.adapt(player)));
    }

    GameEntryService(GameEntryPolicy policy, Consumer<Player> lobbyTransition) {
        this.suppliedPolicy = policy;
        this.lobbyTransition = lobbyTransition;
    }

    public static GameEntryService get() {
        return INSTANCE;
    }

    public void setLobbyTransition(Consumer<Player> transition) {
        lobbyTransition = Objects.requireNonNull(transition);
    }
    public void setRoutingEnabled(boolean enabled) { routingEnabled = enabled; }
    public boolean isRoutingEnabled() { return routingEnabled; }
    public boolean policiesCompatible() {
        try {
            selectPolicy(ChallengeManager.getActiveChallenges().stream().map(Challenge::getEntryPolicy)
                    .filter(Objects::nonNull).toList());
            return true;
        } catch (IllegalStateException exception) {
            return false;
        }
    }

    public void beginRound(UUID roundId, Set<UUID> participants) {
        GameEntryPolicy selected = suppliedPolicy != null ? suppliedPolicy : selectPolicy(ChallengeManager.getActiveChallenges().stream()
                .map(Challenge::getEntryPolicy).filter(Objects::nonNull).toList());
        savedStates.clear();
        pending.clear();
        initialAdmitted.clear();
        shuttingDown = false;
        roundStarted = false;
        restoredPreparationPaused = false;
        this.policy = selected;
        this.roundId = Objects.requireNonNull(roundId);
        this.participants = Set.copyOf(participants);
        this.presence = new GamePresence(roundId);
    }
    static GameEntryPolicy selectPolicy(Collection<GameEntryPolicy> policies) {
        GameEntryPolicy selected = null;
        boolean tied = false;
        for (GameEntryPolicy candidate : policies) {
            if (selected == null || candidate.priority() > selected.priority()) {
                selected = candidate;
                tied = false;
            } else if (candidate.priority() == selected.priority()) {
                tied = true;
            }
        }
        if (tied) throw new IllegalStateException("Equal highest entry policy priorities");
        return selected;
    }
    public void reset() {
        savedStates.clear();
        pending.clear();
        initialAdmitted.clear();
        roundStarted = false;
        restoredPreparationPaused = false;
        shuttingDown = false;
        roundId = null;
        participants = Set.of();
        presence = null;
        policy = null;
    }
    public record Snapshot(int version, UUID roundId, Set<UUID> participants, Map<UUID, SavedGamePlayerState> savedStates,
                           Map<UUID, EntryReason> pending, Boolean initialAdmissionComplete, Set<UUID> initialAdmitted) {
        public Snapshot(int version, UUID roundId, Set<UUID> participants, Map<UUID, SavedGamePlayerState> savedStates) {
            this(version, roundId, participants, savedStates, Map.of(), true, participants);
        }
    }
    public Snapshot snapshot() {
        Map<UUID, EntryReason> requests = new HashMap<>();
        pending.forEach((id, request) -> requests.put(id, request.reason()));
        return new Snapshot(1, roundId, Set.copyOf(participants), Map.copyOf(savedStates), Map.copyOf(requests), roundStarted, Set.copyOf(initialAdmitted));
    }

    public void restore(Snapshot snapshot) {
        if (snapshot == null) {
            reset();
            return;
        }
        if (snapshot.version() != 1 || snapshot.participants() == null || snapshot.savedStates() == null) {
            throw new IllegalStateException("Invalid saved game entry state");
        }
        if (snapshot.roundId() == null) {
            reset();
            savedStates.putAll(snapshot.savedStates());
            return;
        }
        beginRound(snapshot.roundId(), snapshot.participants());
        roundStarted = snapshot.initialAdmissionComplete() == null || snapshot.initialAdmissionComplete();
        restoredPreparationPaused = !roundStarted;
        savedStates.putAll(snapshot.savedStates());
        if (snapshot.initialAdmitted() != null) initialAdmitted.addAll(snapshot.initialAdmitted());
        else if (roundStarted) initialAdmitted.addAll(participants);
        initialAdmitted.retainAll(participants);
        if (snapshot.pending() != null) {
            snapshot.pending().forEach((id, reason) -> {
                if (participants.contains(id) && savedStates.containsKey(id)
                        && (reason == EntryReason.REJOIN || reason == EntryReason.RESTORE)) {
                    presence.transition(id, presence.generation(id), GamePresence.State.PENDING);
                    pending.put(id, new PendingAdmission(roundId, presence.generation(id), reason));
                }
            });
        }
    }
    public void acceptRecoveredState(Player player, Location location) {
        savedStates.put(player.getUniqueId(), SavedGamePlayerState.capture(player, location));
    }
    public void onLogin(Player player) {
        onLogin(player, null);
    }

    public void onLogin(Player player, Location previousLocation) {
        UUID playerId = player.getUniqueId();
        PendingAdmission request = pending.remove(playerId);
        if (!savedStates.containsKey(playerId)) {
            savedStates.put(playerId, previousLocation == null ? SavedGamePlayerState.capture(player)
                    : SavedGamePlayerState.capture(player, previousLocation));
        }
        if (presence != null) presence.transition(playerId, presence.generation(playerId), GamePresence.State.LIMBO);
        enterLimbo(player, LeaveReason.LIMBO);
        if (request != null && presence != null && request.roundId().equals(roundId)) {
            pending.put(playerId, new PendingAdmission(roundId, presence.generation(playerId), request.reason()));
        }
    }
    public void enterLimbo(Player player, LeaveReason reason) {
        UUID playerId = player.getUniqueId();
        callbackGenerations.merge(playerId, 1L, Long::sum);
        if (presence != null && isPlaying(playerId)) {
            savedStates.put(playerId, SavedGamePlayerState.capture(player));
            if (!shuttingDown) notifyDeparture(player, reason);
        } else if (!savedStates.containsKey(playerId)) {
            savedStates.put(playerId, SavedGamePlayerState.capture(player));
        }
        pending.remove(playerId);
        if (presence != null) presence.transition(playerId, presence.generation(playerId), GamePresence.State.LIMBO);
        if (KatzeGames.get() != null && KatzeGames.get().isEnabled()) SessionPersistence.saveNow();
        player.closeInventory();
        lobbyTransition.accept(player);
    }
    public void onQuit(Player player) {
        UUID playerId = player.getUniqueId();
        if (presence == null) return;
        pending.remove(playerId);
        if (isPlaying(playerId)) {
            savedStates.put(playerId, SavedGamePlayerState.capture(player));
            if (!shuttingDown) notifyDeparture(player, LeaveReason.DISCONNECT);
        }
        presence.transition(playerId, presence.generation(playerId), GamePresence.State.LIMBO);
        if (!shuttingDown) SessionPersistence.requestSave();
    }

    private void notifyDeparture(Player player, LeaveReason reason) {
        if (policy != null) policy.left(player.getUniqueId(), reason);
        for (Challenge challenge : ChallengeManager.getActiveChallenges()) challenge.onPlayerLeaving(player, reason);
    }
    public boolean isShuttingDown() { return shuttingDown; }
    public void shutdown() {
        shuttingDown = true;
        capturePlaying();
    }

    public void capturePlaying() {
        if (presence == null) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isPlaying(player.getUniqueId())) savedStates.put(player.getUniqueId(), SavedGamePlayerState.capture(player));
        }
    }
    public void tick() {
        if (!routingEnabled || roundId == null || !ChallengeManager.isGameStarted()) return;
        if (!roundStarted && !restoredPreparationPaused) completeRoundStart();
        else if (KatzeGames.getTimer().isRunning() && !roundFinished()) resumePending();
    }
    public boolean blocksTimerStart() { return routingEnabled && roundId != null && !roundStarted; }
    public void resumePreparedRound() {
        if (roundStarted || !restoredPreparationPaused) return;
        restoredPreparationPaused = false;
        completeRoundStart();
    }
    public void completeRoundStart() {
        if (roundId == null || roundStarted || (policy != null && policy.preparing())) return;
        UUID expectedRound = roundId;
        startingAdmissions = true;
        try {
            for (UUID id : participants) {
                Player player = Bukkit.getPlayer(id);
                if (player == null || isPlaying(id) || SpectatorManager.isSpectator(id)) continue;
                EntryReason reason = initialAdmitted.contains(id) ? EntryReason.RESTORE : EntryReason.ROUND_START;
                if (enter(player, expectedRound, reason) != EntryResult.ENTERED) return;
                if (!expectedRound.equals(roundId)) return;
            }
        } finally {
            startingAdmissions = false;
        }
        roundStarted = true;
        if (KatzeGames.getTimer().getMode() != GameTimer.Mode.DISABLED) KatzeGames.getTimer().start();
        if (KatzeGames.get() != null && KatzeGames.get().isEnabled()) SessionPersistence.requestSave();
    }
    public void resumePending() {
        for (Map.Entry<UUID, PendingAdmission> entry : Map.copyOf(pending).entrySet()) {
            UUID playerId = entry.getKey();
            PendingAdmission request = entry.getValue();
            if (presence == null || !request.roundId().equals(roundId)
                    || request.generation() != presence.generation(playerId)) {
                pending.remove(playerId, request);
                continue;
            }
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && pending.remove(playerId, request)) enter(player, request.roundId(), request.reason());
        }
    }
    public UUID currentRoundId() {
        return roundId;
    }
    public long generation(UUID playerId) {
        return callbackGenerations.getOrDefault(playerId, 0L);
    }

    public boolean roundFinished() {
        return KatzeGames.getTimer().hasHitZero() || (policy != null && policy.finished())
                || ChallengeManager.getActiveChallenges().stream().anyMatch(Challenge::isRoundFinished);
    }
    public int playingCount() {
        return (int) participants.stream().filter(this::isPlaying).count();
    }
    public boolean isEligible(UUID playerId) {
        return roundId != null && participants.contains(playerId) && !SpectatorManager.isSpectator(playerId);
    }
    public boolean isViewing(UUID playerId) {
        return presence != null && presence.state(playerId) == GamePresence.State.VIEWING;
    }
    public boolean isPlaying(UUID playerId) {
        return presence != null && participants.contains(playerId) && !SpectatorManager.isSpectator(playerId)
                && presence.isPlaying(playerId);
    }

    public boolean allowsProgress(UUID playerId) {
        return roundId == null || canProgress(playerId);
    }
    public boolean canProgress(UUID playerId) {
        if (!isPlaying(playerId) || !ChallengeManager.isGameStarted()) return false;
        GameTimer timer = KatzeGames.getTimer();
        return !timer.hasHitZero() && (timer.getMode() == GameTimer.Mode.DISABLED || timer.isRunning());
    }
    public EntryResult rejoin(Player player) {
        return enter(player, roundId, EntryReason.REJOIN);
    }
    public EntryResult enter(Player player, UUID requestedRound, EntryReason reason) {
        if (roundId == null || !roundId.equals(requestedRound)) return EntryResult.DENIED;
        if (reason != EntryReason.ROUND_START && roundFinished()) return EntryResult.DENIED;
        if (reason != EntryReason.SPECTATE && !participants.contains(player.getUniqueId())) return EntryResult.DENIED;
        UUID playerId = player.getUniqueId();
        if (presence.isPlaying(playerId) || presence.state(playerId) == GamePresence.State.VIEWING) return EntryResult.DENIED;
        if (reason == EntryReason.SPECTATE) {
            if (!ChallengeManager.isGameStarted()) return EntryResult.DENIED;
            return spectate(player, requestedRound);
        }
        if (SpectatorManager.isSpectator(playerId)) return EntryResult.DENIED;
        if ((reason == EntryReason.REJOIN || reason == EntryReason.RESTORE) && !savedStates.containsKey(playerId)) return EntryResult.DENIED;
        GameTimer timer = KatzeGames.getTimer();
        if (!startingAdmissions && (reason == EntryReason.REJOIN || reason == EntryReason.RESTORE) && timer.getMode() != GameTimer.Mode.DISABLED
                && !timer.isRunning() && !timer.hasHitZero()) {
            long generation = presence.generation(playerId);
            if (!presence.transition(playerId, generation, GamePresence.State.PENDING)) return EntryResult.DENIED;
            pending.put(playerId, new PendingAdmission(roundId, presence.generation(playerId), reason));
            return EntryResult.QUEUED;
        }
        if (reason == EntryReason.REJOIN && !initialAdmitted.contains(playerId)) reason = EntryReason.ROUND_START;
        GameEntryPolicy.Admission admission = policy == null ? null : policy.prepare(playerId, reason);
        if (admission != null && !admission.allowed()) return EntryResult.DENIED;
        GamePresence expectedPresence = presence;
        long generation = presence.generation(playerId);
        SavedGamePlayerState temporary = null;
        try {
            temporary = SavedGamePlayerState.capture(player);
            if (reason == EntryReason.ROUND_START) {
                savedStates.put(playerId, temporary);
                if (KatzeGames.get() != null && KatzeGames.get().isEnabled()) SessionPersistence.saveNow();
                World world = admission == null ? GameWorldManager.getWorld() : null;
                Location destination = admission == null ? (world == null ? null : world.getSpawnLocation()) : admission.destination();
                if (destination == null || !player.teleport(destination)) return EntryResult.DENIED;
                if (!requestedRound.equals(roundId) || expectedPresence != presence || expectedPresence.generation(playerId) != generation) return EntryResult.DENIED;
                player.closeInventory();
                player.setItemOnCursor(null);
                player.getInventory().clear();
                player.setGameMode(GameMode.SURVIVAL);
                player.setHealth(20);
                player.setFoodLevel(20);
                player.setSaturation(20);
                player.setFireTicks(0);
            } else if (reason == EntryReason.REJOIN || reason == EntryReason.RESTORE) {
                SavedGamePlayerState saved = savedStates.get(playerId);
                if (saved == null) return EntryResult.DENIED;
                saved.restore(player, admission == null ? null : admission.destination(),
                        () -> expectedPresence == presence && requestedRound.equals(roundId) && expectedPresence.generation(playerId) == generation);
            } else {
                return EntryResult.DENIED;
            }
            if (!requestedRound.equals(roundId) || expectedPresence != presence || expectedPresence.generation(playerId) != generation) return EntryResult.DENIED;
            if (policy != null) policy.admitted(playerId, reason);
            for (Challenge challenge : ChallengeManager.getActiveChallenges()) challenge.onPlayerAdmitted(player, reason);
            if (!requestedRound.equals(roundId) || expectedPresence != presence || expectedPresence.generation(playerId) != generation) return EntryResult.DENIED;
            if (reason == EntryReason.ROUND_START) savedStates.put(playerId, SavedGamePlayerState.capture(player));
            if (!requestedRound.equals(roundId) || expectedPresence != presence
                    || !expectedPresence.transition(playerId, generation, GamePresence.State.PLAYING)) return EntryResult.DENIED;
            if (reason == EntryReason.ROUND_START) {
                initialAdmitted.add(playerId);
                if (KatzeGames.get() != null && KatzeGames.get().isEnabled()) {
                    try { SessionPersistence.saveNow(); }
                    catch (RuntimeException exception) { KatzeGames.get().getLogger().log(java.util.logging.Level.SEVERE, "Failed to save completed admission", exception); }
                }
            }
            return EntryResult.ENTERED;
        } catch (RuntimeException exception) {
            if (temporary != null && expectedPresence == presence && requestedRound.equals(roundId)
                    && expectedPresence.generation(playerId) == generation) {
                try { temporary.restore(player); } catch (RuntimeException rollbackFailure) {
                    player.kick(net.kyori.adventure.text.Component.translatable("katze.game.start.recovery_failed"));
                }
            }
            return EntryResult.DENIED;
        }
    }
    private EntryResult spectate(Player player, UUID requestedRound) {
        UUID id = player.getUniqueId();
        Location target = policy == null ? null : policy.spectatorTargets().stream().findFirst().orElse(null);
        if (target == null) {
            target = Bukkit.getOnlinePlayers().stream().filter(other -> isPlaying(other.getUniqueId()))
                    .map(Player::getLocation).findFirst().orElseGet(() -> GameWorldManager.getWorld().getSpawnLocation());
        }
        GamePresence expected = presence;
        long generation = expected.generation(id);
        if (!player.teleport(target) || expected != presence || !requestedRound.equals(roundId)
                || expected.generation(id) != generation) return EntryResult.DENIED;
        player.setGameMode(GameMode.SPECTATOR);
        if (!expected.transition(id, generation, GamePresence.State.VIEWING)) return EntryResult.DENIED;
        if (policy != null) policy.admitted(id, EntryReason.SPECTATE);
        return EntryResult.ENTERED;
    }
}
