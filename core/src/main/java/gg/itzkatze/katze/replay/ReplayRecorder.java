package gg.itzkatze.katze.replay;

import com.destroystokyo.paper.profile.ProfileProperty;
import gg.itzkatze.katze.KatzeGames;
import gg.itzkatze.katze.challenges.ChallengeManager;
import gg.itzkatze.katze.challenges.ChallengeType;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Item;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

final class ReplayRecorder {
    static final List<EquipmentSlot> EQUIPMENT = List.of(EquipmentSlot.HAND, EquipmentSlot.OFF_HAND, EquipmentSlot.HEAD,
            EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
    private final UUID round;
    private final List<UUID> participants;
    private final ReplayStore store;
    private final Executor executor;
    private final Set<UUID> known = new HashSet<>();
    private final ReplayTerrainSchedule terrainSchedule = new ReplayTerrainSchedule();
    private final String minecraft = Bukkit.getMinecraftVersion();
    private final boolean resumed;
    private final ReplayTerrain chunks;
    private final Map<UUID, List<String>> equipment = new HashMap<>();
    private final Map<BlockData, String> blockStates = new HashMap<>();
    private final ReplayPlayerSnapshots playerSnapshots = new ReplayPlayerSnapshots();
    private final ReplayContainerSnapshots containers = new ReplayContainerSnapshots();
    private final Map<UUID, ItemStack> itemStacks = new HashMap<>();
    private final Set<BlockPosition> dirty = new HashSet<>();
    private final ReplayRecordingQueue queue;
    private final Set<UUID> pendingSwings = new HashSet<>();
    private final Set<UUID> pendingHurt = new HashSet<>();
    private ReplayArchive.Writer writer;
    private ReplayCheckpoint.Builder checkpoint;
    private boolean recordedParticipant;
    private volatile Throwable failure;
    private int tick;
    private Integer combatTick;
    private boolean stopped;
    private CompletableFuture<Void> completion;

    private record Terrain(ReplayStore.ChunkKey key, ChunkSnapshot snapshot, int minY, int maxY) {}
    private record BlockPosition(UUID world, int x, int y, int z) {
        ReplayStore.ChunkKey chunk() { return new ReplayStore.ChunkKey(world, x >> 4, z >> 4); }
    }
    private record ChangedBlock(BlockPosition position, String material) {}

    void changed(Block block) {
        BlockPosition position = new BlockPosition(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
        if (terrainSchedule.contains(position.chunk())) dirty.add(position);
    }

    void interactContainer(Player player, Block block) {
        if (participants.contains(player.getUniqueId())) containers.interact(player.getUniqueId(), block);
    }

    ReplayRecorder(UUID round, List<UUID> participants, ReplayStore store, Executor executor) throws IOException {
        this.round = round;
        this.participants = List.copyOf(participants);
        this.store = store;
        this.executor = executor;
        queue = new ReplayRecordingQueue(executor);
        ReplayStore.Info previous = store.info(round);
        resumed = previous != null;
        chunks = new ReplayTerrain(store.path(round).resolve("state"), store.path(round).resolve("chunks"), resumed);
        if (ChallengeManager.getActiveChallenges().stream().anyMatch(challenge -> challenge.getType() == ChallengeType.MOB_ARMY)) checkpoint = new ReplayCheckpoint.Builder();
        if (resumed) {
            if (previous.format() < 1 || previous.format() > 4 || !minecraft.equals(previous.minecraft()) || !Set.copyOf(participants).equals(Set.copyOf(previous.participants()))) {
                throw new IOException("Cannot resume replay from a different version or roster");
            }
            tick = previous.duration();
            combatTick = previous.combatTick();
            checkpoint = null;
        }
    }

    void tick(Set<UUID> swings) {
        capture(swings, false);
    }

    void markCombat() { if (combatTick == null) combatTick = tick; }

    void capture(Set<UUID> swings, boolean finalFrame) {
        capture(swings, Set.of(), List.of(), finalFrame);
    }

    void capture(Set<UUID> swings, Set<UUID> hurt, List<ReplayFrame.Mining> mining, boolean finalFrame) {
        if (stopped || failure != null) return;
        pendingSwings.addAll(swings);
        pendingHurt.addAll(hurt);
        if (!queue.submit(() -> captureFrame(mining, finalFrame), finalFrame)) tick++;
    }

    private Runnable captureFrame(List<ReplayFrame.Mining> mining, boolean finalFrame) {
        int current = tick;
        Map<ReplayStore.ChunkKey, Chunk> visible = new LinkedHashMap<>();
        Map<UUID, World> worlds = new LinkedHashMap<>();
        List<Player> players = new ArrayList<>();
        int radius = Math.clamp(KatzeGames.get().getConfig().getInt("replays.chunk-radius", 2), 1, 8);
        for (UUID id : participants) {
            Player player = Bukkit.getPlayer(id);
            if (player == null || ReplayService.isReplayWorld(player.getWorld())) continue;
            players.add(player);
            World world = player.getWorld();
            worlds.put(world.getUID(), world);
            Chunk currentChunk = player.getChunk();
            visible.put(new ReplayStore.ChunkKey(world.getUID(), currentChunk.getX(), currentChunk.getZ()), currentChunk);
            int cx = player.getLocation().getBlockX() >> 4, cz = player.getLocation().getBlockZ() >> 4;
            for (int x = cx - radius; x <= cx + radius; x++) for (int z = cz - radius; z <= cz + radius; z++) {
                if (world.isChunkLoaded(x, z)) visible.put(new ReplayStore.ChunkKey(world.getUID(), x, z), world.getChunkAt(x, z));
            }
        }
        List<Entity> tracked = ChallengeManager.getActiveChallenges().stream().flatMap(challenge -> challenge.getReplayEntities(round).stream())
                .filter(entity -> entity.isValid() && !entity.isDead() && !ReplayService.isReplayWorld(entity.getWorld())).toList();
        for (Entity entity : tracked) {
            World world = entity.getWorld();
            worlds.put(world.getUID(), world);
            Location location = entity.getLocation();
            int cx = location.getBlockX() >> 4, cz = location.getBlockZ() >> 4;
            for (int x = cx - 1; x <= cx + 1; x++) for (int z = cz - 1; z <= cz + 1; z++) {
                if (world.isChunkLoaded(x, z)) visible.putIfAbsent(new ReplayStore.ChunkKey(world.getUID(), x, z), world.getChunkAt(x, z));
            }
        }
        if (worlds.isEmpty()) return null;
        if (finalFrame) playerSnapshots.invalidate();
        List<ReplayFrame.Container> openContainers = containers.capture(players);
        List<ReplayFrame.Actor> actors = ReplayActors.capture(players, visible.values(), tracked, entity -> actor(entity, pendingSwings.contains(entity.getUniqueId()), pendingHurt.contains(entity.getUniqueId())));
        pendingSwings.clear();
        pendingHurt.clear();
        Set<UUID> visibleActors = new HashSet<>();
        actors.forEach(actor -> visibleActors.add(actor.id()));
        equipment.keySet().retainAll(visibleActors);
        itemStacks.keySet().retainAll(visibleActors);
        List<Terrain> terrain = new ArrayList<>();
        for (var key : terrainSchedule.capture(visible.keySet(), current, finalFrame, queue.canRefreshTerrain())) {
            Chunk chunk = visible.get(key);
            terrain.add(new Terrain(key, chunk.getChunkSnapshot(false, true, false), chunk.getWorld().getMinHeight(), chunk.getWorld().getMaxHeight()));
        }
        List<ReplayFrame.Scene> scenes = worlds.values().stream().map(world -> new ReplayFrame.Scene(world.getUID(), world.getEnvironment().name(),
                world.getTime(), world.hasStorm(), world.isThundering())).toList();
        List<ChangedBlock> changed = new ArrayList<>();
        for (BlockPosition position : dirty) {
            World world = Bukkit.getWorld(position.world());
            if (world != null && world.isChunkLoaded(position.x() >> 4, position.z() >> 4)) {
                changed.add(new ChangedBlock(position, world.getBlockAt(position.x(), position.y(), position.z()).getBlockData().getAsString()));
            }
        }
        dirty.clear();
        tick++;
        return () -> {
            try {
                if (failure != null) return;
                if (writer == null) {
                    writer = store.writer(round, resumed);
                }
                List<ReplayFrame.BlockChange> changes = new ArrayList<>();
                for (ChangedBlock block : changed) {
                    BlockPosition at = block.position();
                    ReplayChunk chunk = chunks.get(at.chunk());
                    if (chunk != null) {
                        ReplayFrame.BlockChange change = chunk.update(at.world(), at.x(), at.y(), at.z(), block.material());
                        if (change != null) {
                            chunks.changed(at.chunk());
                            changes.add(change);
                        }
                    }
                }
                for (Terrain image : terrain) {
                    ReplayChunk next = ReplayChunk.capture(image.snapshot(), image.minY(), image.maxY(), blockStates);
                    ReplayChunk previous = chunks.replace(image.key(), next);
                    if (previous == null) store.writeChunk(round, image.key(), next);
                    else changes.addAll(next.changes(image.key().world(), image.key().x(), image.key().z(), previous));
                }
                ReplayFrame frame = new ReplayFrame(current, actors, changes, scenes, mining.stream().filter(attempt -> participants.contains(attempt.player())).toList(), openContainers);
                store.writeFrame(writer, frame);
                if (checkpoint != null) {
                    checkpoint.add(frame);
                    if (combatTick != null && current >= combatTick) {
                        checkpoint.build().write(store.path(round).resolve("combat.bin"));
                        checkpoint = null;
                    }
                }
                recordedParticipant |= frame.hasParticipant(participants);
            } catch (Exception e) {
                failure = e;
            }
        };
    }

    private ReplayFrame.Actor actor(Entity entity, boolean swinging, boolean hurt) {
        boolean first = known.add(entity.getUniqueId());
        Player player = entity instanceof Player p ? p : null;
        ProfileProperty texture = first && player != null ? player.getPlayerProfile().getProperties().stream()
                .filter(property -> property.getName().equals("textures")).findFirst().orElse(null) : null;
        Location location = entity.getLocation();
        List<String> items = new ArrayList<>();
        if (entity instanceof LivingEntity living && living.getEquipment() != null) {
            for (EquipmentSlot slot : EQUIPMENT) {
                ItemStack item = living.getEquipment().getItem(slot);
                items.add(item == null || item.isEmpty() ? "" : Base64.getEncoder().encodeToString(item.serializeAsBytes()));
            }
        }
        List<String> previous = equipment.put(entity.getUniqueId(), items);
        String snapshot = null;
        if (first && player == null) {
            var image = entity.createSnapshot();
            if (image != null) snapshot = image.getAsString();
        }
        String item = null;
        if (entity instanceof Item dropped) {
            ItemStack stack = dropped.getItemStack();
            if (!stack.equals(itemStacks.get(entity.getUniqueId()))) {
                itemStacks.put(entity.getUniqueId(), stack.clone());
                item = ReplayItems.encode(stack);
            }
        }
        Entity vehicle = entity.getVehicle();
        double health = entity instanceof LivingEntity living ? living.getHealth() : 0;
        double maxHealth = entity instanceof LivingEntity living ? living.getMaxHealth() : 0;
        return new ReplayFrame.Actor(entity.getUniqueId(), location.getWorld().getUID(), entity.getType().name(),
                player == null ? null : player.getName(), snapshot, texture == null ? null : texture.getValue(),
                texture == null ? null : texture.getSignature(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch(),
                entity.getPose().name(), entity.getFireTicks() > 0, swinging, items.equals(previous) ? null : List.copyOf(items),
                vehicle == null ? null : vehicle.getUniqueId(), hurt, entity instanceof LivingEntity living ? living.getHurtDirection() : 0,
                item, player == null ? null : playerSnapshots.capture(player, tick), health, maxHealth);
    }

    CompletableFuture<Void> finish() {
        if (completion != null) return completion;
        stopped = true;
        completion = new CompletableFuture<>();
        executor.execute(() -> {
            try {
                if (writer != null) writer.close();
                if (failure != null) throw new IOException("Failed to record replay " + round, failure);
                if (writer != null) {
                    if (!recordedParticipant) throw new IOException("Replay contains no recorded participants: " + round);
                    chunks.flush();
                    store.saveInfo(new ReplayStore.Info(4, minecraft, round, participants, tick, combatTick));
                }
                completion.complete(null);
            } catch (Exception e) {
                completion.completeExceptionally(e);
            } finally {
                chunks.clear();
                blockStates.clear();
            }
        });
        return completion;
    }
}
