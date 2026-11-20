package gg.itzkatze.katze.replay;

import com.destroystokyo.paper.profile.ProfileProperty;
import gg.itzkatze.katze.results.SavedBattle;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.TriState;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Pose;
import org.bukkit.entity.Item;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.bukkit.block.data.BlockData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntSupplier;

final class ReplayPlayback implements AutoCloseable {
    private final ReplayStore.Loaded replay;
    private final SavedBattle battle;
    private final ReplayWorlds worlds;
    private ReplayTimeline timeline;
    private final ReplayTransport transport = new ReplayTransport();
    private final ReplayAudience<ReplaySession> audience = new ReplayAudience<>();
    private final ReplayContainers containers;
    private final Map<UUID, Entity> actors = new HashMap<>();
    private final Map<UUID, List<String>> appliedEquipment = new HashMap<>();
    private final Map<UUID, String> appliedItems = new HashMap<>();
    private boolean initialized;
    private CompletableFuture<ReplaySeek> seeking;
    private ReplaySeek pendingSeek;
    private Integer combatTick;
    private final Map<String, BlockData> blockData = new HashMap<>();
    private final ReplayTerrainState physical = new ReplayTerrainState();
    private Set<UUID> deferredWorlds = Set.of();

    ReplayPlayback(ReplayStore.Loaded replay, SavedBattle battle) {
        this.replay = replay;
        this.battle = battle;
        timeline = new ReplayTimeline(replay.frames(), replay.info().duration());
        worlds = new ReplayWorlds(replay);
        containers = new ReplayContainers(worlds::get);
    }

    void addViewer(ReplaySession session) { audience.add(session); }
    void removeViewer(ReplaySession session) throws IOException { if (audience.remove(session)) close(); }
    boolean closed() { return audience.closed(); }

    void initialize() {
        if (initialized) return;
        advance(replay.startFrame().tick());
        initialized = true;
    }

    void tick(int tick) {
        if (!transport.claimTick(tick)) return;
        if (seeking()) {
            finishSeek();
            return;
        }
        if (!paused()) advance(timeline.position() + speed(), true);
        else if (timeline.currentFrame() != null) render(timeline.currentFrame());
        if (timeline.position() >= duration() && !paused()) {
            transport.pause();
            refreshViewers();
        }
    }

    void seek(int seconds) {
        int target = Math.clamp((long) seconds * 20, 0, duration());
        seek(() -> target);
    }

    void seekCombat() {
        if (battle.game() != SavedBattle.Game.MOB_ARMY) return;
        seek(() -> {
            if (combatTick == null) combatTick = replay.combat() == null ? -1 : replay.combat().frame().tick();
            return combatTick;
        });
    }

    private void seek(IntSupplier target) {
        if (seeking() || closed()) return;
        audience.viewers().forEach(ReplaySession::prepareSeek);
        containers.clear();
        ReplayTimeline source = timeline;
        seeking = ReplayService.get().read(() -> {
            int tick = target.getAsInt();
            return tick < 0 ? null : ReplaySeek.prepare(source, tick, physical, replay.combat(), replay.frames(), duration());
        });
    }

    boolean seeking() { return seeking != null; }

    private void finishSeek() {
        if (!seeking.isDone()) return;
        if (pendingSeek == null) pendingSeek = seeking.join();
        if (pendingSeek == null) {
            seeking = null;
            audience.viewers().forEach(ReplaySession::combatUnavailable);
            return;
        }
        if (!pendingSeek.apply(128, 3_000_000, System::nanoTime, block -> apply(block, false))) return;
        timeline = pendingSeek.timeline();
        deferredWorlds = pendingSeek.deferredWorlds();
        seeking = null;
        pendingSeek = null;
        ReplayFrame frame = timeline.currentFrame();
        if (frame != null) {
            frame.scenes().forEach(scene -> ReplayWorlds.applyScene(worlds.get(scene.world()), scene));
            render(frame);
        }
        for (ReplaySession viewer : audience.viewers()) {
            viewer.relocateAfterSeek();
            viewer.refreshPlaybackView();
            viewer.refreshSharedControls();
        }
    }

    private void refreshViewers() { audience.viewers().forEach(ReplaySession::refreshSharedControls); }
    void togglePause() { transport.togglePause(); refreshViewers(); }
    void cycleSpeed() { transport.cycleSpeed(); refreshViewers(); }
    int duration() { return replay.info().duration(); }
    int seconds() { return (int) timeline.position() / 20; }
    boolean paused() { return transport.paused(); }
    double speed() { return transport.speed(); }
    SavedBattle battle() { return battle; }
    World world(UUID source) { return worlds.get(source); }
    boolean owns(World world) { return worlds.contains(world); }
    ReplayFrame.Actor startActor(UUID viewer) { return replay.startActor(viewer); }
    Entity actor(UUID id) { return actors.get(id); }
    ReplayFrame.Actor definition(UUID id) { return timeline.definition(id); }
    ReplayFrame.PlayerState player(UUID id) { return timeline.player(id); }
    List<String> equipment(UUID id) { return timeline.equipment(id); }
    List<ReplayFrame.Actor> players() { return timeline.players(); }
    List<ReplayFrame.Mining> mining() { return timeline.currentFrame() == null ? List.of() : timeline.currentFrame().mining(); }
    UUID playerId(Entity clicked) {
        return actors.entrySet().stream().filter(entry -> entry.getValue() == clicked).map(Map.Entry::getKey)
                .filter(id -> definition(id) != null && "PLAYER".equals(definition(id).type())).findFirst().orElse(null);
    }
    Location startPosition(UUID viewer, UUID perspective) {
        Entity current = actors.get(perspective);
        return current == null ? location(replay.startActor(viewer)).add(0, 2, 0) : current.getLocation().add(0, 2, 0);
    }

    private void advance(double target) {
        advance(target, false);
    }

    private void advance(double target, boolean animate) {
        ReplayEffects effects = new ReplayEffects();
        ReplayFrame last = timeline.moveTo(target, (block, reverse) -> {
            if (!deferredWorlds.contains(block.world())) apply(block, reverse);
        }, frame -> {
            if (animate) effects.capture(frame);
        });
        if (timeline.currentFrame() != null && timeline.currentFrame().actors().stream().anyMatch(actor -> deferredWorlds.contains(actor.world()))) {
            seek(() -> (int) timeline.position());
            return;
        }
        if (last != null) {
            for (ReplayFrame.Scene scene : last.scenes()) {
                ReplayWorlds.applyScene(worlds.get(scene.world()), scene);
            }
        }
        if (timeline.currentFrame() != null) render(timeline.currentFrame());
        effects.play(actors);
    }

    private void apply(ReplayFrame.BlockChange block, boolean backwards) {
        String state = backwards ? block.before() : block.after();
        worlds.get(block.world()).getBlockAt(block.x(), block.y(), block.z()).setBlockData(blockData.computeIfAbsent(state, Bukkit::createBlockData), false);
        physical.apply(block, backwards);
    }

    private Location location(ReplayFrame.Actor actor) {
        return new Location(worlds.get(actor.world()), actor.x(), actor.y(), actor.z(), actor.yaw(), actor.pitch());
    }

    private Location smoothLocation(ReplayFrame.Actor actor) {
        ReplayPosition position = ReplayPosition.sample(actor, timeline.nextActor(actor.id()), timeline.fraction());
        Location location = new Location(worlds.get(actor.world()), position.x(), position.y(), position.z(), position.yaw(), position.pitch());
        if (actor.type().equals("ITEM")) {
            location.add(0, 0.15 + Math.sin(timeline.position() / 10 + actor.id().hashCode()) * 0.03, 0);
            location.setYaw((float) (timeline.position() * 3));
        }
        return location;
    }

    private Entity spawn(ReplayFrame.Actor state) {
        ReplayFrame.Actor definition = timeline.definition(state.id());
        Location location = location(state);
        Entity entity;
        if (state.type().equals("ITEM")) {
            ItemStack item = ReplayItems.decode(timeline.item(state.id()));
            if (item == null && definition.snapshot() != null) {
                Entity original = Bukkit.getEntityFactory().createEntitySnapshot(ReplayEntitySnapshots.prepare(definition, state)).createEntity(location);
                if (original instanceof Item dropped) item = dropped.getItemStack().clone();
                original.remove();
            }
            ItemStack displayed = item == null ? new ItemStack(Material.PAPER) : item;
            entity = location.getWorld().spawn(location, ItemDisplay.class, display -> {
                display.setItemStack(displayed);
                display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GROUND);
                display.setTeleportDuration(1);
                display.setInterpolationDuration(1);
                display.setShadowRadius(0.15f);
            });
        } else if (state.type().equals("PLAYER")) {
            entity = location.getWorld().spawn(location, Mannequin.class, mannequin -> {
                var profile = Bukkit.createProfile(state.id(), definition.name());
                if (definition.texture() != null) profile.setProperty(new ProfileProperty("textures", definition.texture(), definition.signature()));
                mannequin.setProfile(ResolvableProfile.resolvableProfile(profile));
                mannequin.customName(Component.text(definition.name()));
                mannequin.setCustomNameVisible(true);
                mannequin.setDescription(Component.empty());
            });
        } else if (definition.snapshot() != null) {
            entity = Bukkit.getEntityFactory().createEntitySnapshot(ReplayEntitySnapshots.prepare(definition, state)).createEntity(location);
        } else {
            entity = location.getWorld().spawnEntity(location, EntityType.valueOf(state.type()));
        }
        entity.setPersistent(false);
        removePassengers(entity);
        entity.setInvulnerable(true);
        entity.setGravity(false);
        entity.setSilent(true);
        if (entity instanceof Item item) {
            item.setUnlimitedLifetime(true);
            item.setPickupDelay(Integer.MAX_VALUE);
        }
        if (entity instanceof TNTPrimed tnt) tnt.setFuseTicks(Integer.MAX_VALUE);
        if (entity instanceof LivingEntity living) {
            living.setAI(false);
            living.setCollidable(false);
            living.setRemoveWhenFarAway(false);
            living.setCanPickupItems(false);
        }
        return entity;
    }

    private static void removePassengers(Entity entity) {
        for (Entity passenger : entity.getPassengers()) { removePassengers(passenger); passenger.remove(); }
        entity.eject();
    }

    private void render(ReplayFrame frame) {
        containers.render(frame.containers());
        Set<UUID> present = new HashSet<>();
        for (ReplayFrame.Actor state : frame.actors()) {
            present.add(state.id());
            Entity entity = actors.get(state.id());
            if (entity == null || !entity.isValid() || !entity.getWorld().equals(worlds.get(state.world()))) {
                if (entity != null) entity.remove();
                entity = spawn(state);
                actors.put(state.id(), entity);
                appliedEquipment.remove(state.id());
                appliedItems.remove(state.id());
            }
        }
        ReplayMounts.detachChanged(actors, frame.actors());
        for (ReplayFrame.Actor state : frame.actors()) {
            Entity entity = actors.get(state.id());
            Location destination = smoothLocation(state);
            if (state.vehicle() == null || !actors.containsKey(state.vehicle())) entity.teleport(destination);
            else entity.setRotation(destination.getYaw(), destination.getPitch());
            entity.setVelocity(new Vector());
            Pose pose = Pose.valueOf(state.pose());
            if (entity.getPose() != pose && (!(entity instanceof Mannequin) || Mannequin.validPoses().contains(pose))) entity.setPose(pose, true);
            entity.setVisualFire(TriState.byBoolean(state.burning()));
            if (entity instanceof LivingEntity living) {
                List<String> items = timeline.equipment(state.id());
                if (items != null && !items.equals(appliedEquipment.get(state.id())) && living.getEquipment() != null) {
                    for (int i = 0; i < items.size(); i++) living.getEquipment().setItem(ReplayRecorder.EQUIPMENT.get(i),
                            items.get(i).isEmpty() ? new ItemStack(Material.AIR) : ItemStack.deserializeBytes(Base64.getDecoder().decode(items.get(i))), true);
                    appliedEquipment.put(state.id(), items);
                }
            }
            String item = timeline.item(state.id());
            if (entity instanceof ItemDisplay display && item != null && !item.equals(appliedItems.get(state.id()))) {
                ItemStack stack = ReplayItems.decode(item);
                if (stack != null) display.setItemStack(stack);
                appliedItems.put(state.id(), item);
            }
        }
        ReplayMounts.attach(actors, frame.actors());
        for (UUID id : new ArrayList<>(actors.keySet())) if (!present.contains(id)) actors.remove(id).remove();
    }


    @Override
    public void close() throws IOException {
        if (seeking != null) seeking.cancel(true);
        seeking = null;
        pendingSeek = null;
        containers.clear();
        actors.values().forEach(Entity::remove);
        actors.clear();
        worlds.close();
    }
}
