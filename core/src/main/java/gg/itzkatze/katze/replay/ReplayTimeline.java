package gg.itzkatze.katze.replay;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

final class ReplayTimeline {
    private final List<ReplayFrame> frames;
    private final int duration;
    private ReplayStateIndex states;
    private ReplayTerrainState terrain = new ReplayTerrainState();
    private int originTick = -1;
    private final Map<UUID, ReplayFrame.Actor> definitions = new HashMap<>();
    private final Map<UUID, List<String>> equipment = new HashMap<>();
    private final Map<UUID, ReplayFrame.PlayerState> players = new HashMap<>();
    private final Map<UUID, String> items = new HashMap<>();
    private final Map<UUID, ReplayFrame.Actor> nextActors = new HashMap<>();
    private int index = -1;
    private double position;
    private ReplayFrame current;
    private ReplayFrame next;

    ReplayTimeline(List<ReplayFrame> frames, int duration) {
        if (frames.isEmpty() || frames.getFirst().tick() != 0 || duration < 0) throw new IllegalArgumentException("Invalid replay timeline");
        if (!(frames instanceof ReplayArchive)) {
            int previous = -1;
            for (ReplayFrame frame : frames) {
                if (frame.tick() <= previous || frame.tick() > duration) throw new IllegalArgumentException("Unordered replay timeline");
                previous = frame.tick();
            }
        }
        this.frames = frames;
        this.duration = duration;
        states = new ReplayStateIndex();
    }

    private ReplayTimeline(ReplayTimeline source) {
        frames = source.frames;
        duration = source.duration;
        states = source.states;
        terrain = source.terrain.copy();
        originTick = source.originTick;
        definitions.putAll(source.definitions);
        equipment.putAll(source.equipment);
        players.putAll(source.players);
        items.putAll(source.items);
        nextActors.putAll(source.nextActors);
        index = source.index;
        position = source.position;
        current = source.current;
        next = source.next;
    }

    ReplayTimeline copy() { return new ReplayTimeline(this); }

    ReplayTimeline(List<ReplayFrame> frames, int duration, ReplayCheckpoint checkpoint) {
        this.frames = frames;
        this.duration = duration;
        states = new ReplayStateIndex();
        index = checkpoint.index();
        remember(checkpoint.seed());
        checkpoint.seed().blocks().forEach(block -> terrain.apply(block, false));
        current = checkpoint.frame();
        position = current.tick();
        originTick = current.tick();
        next = index + 1 < frames.size() ? frames.get(index + 1) : null;
        if (next != null) next.actors().forEach(actor -> nextActors.put(actor.id(), actor));
    }

    ReplayTerrainState terrain() { return terrain; }

    ReplayFrame moveTo(double target, BiConsumer<ReplayFrame.BlockChange, Boolean> apply) {
        return moveTo(target, apply, frame -> {});
    }

    ReplayFrame moveTo(double target, BiConsumer<ReplayFrame.BlockChange, Boolean> apply, Consumer<ReplayFrame> crossed) {
        target = Math.clamp(target, 0, duration);
        if (originTick >= 0 && target < originTick) return resetTo(target, apply);
        ReplayFrame last = null;
        if (target < position && current != null && current.tick() > target) {
            Set<ReplayStateIndex.Change> changed = new HashSet<>();
            while (index >= 0 && frames.get(index).tick() > target) {
                if (Thread.currentThread().isInterrupted()) throw new CancellationException();
                ReplayFrame frame = frames.get(index);
                for (ReplayFrame.Actor actor : frame.actors()) {
                    if (states.firstAppearance(actor.id(), index)) definitions.remove(actor.id());
                    if (actor.equipment() != null) changed.add(new ReplayStateIndex.Change(actor.id(), ReplayStateIndex.Property.EQUIPMENT));
                    if (actor.player() != null) changed.add(new ReplayStateIndex.Change(actor.id(), ReplayStateIndex.Property.PLAYER));
                    if (actor.item() != null) changed.add(new ReplayStateIndex.Change(actor.id(), ReplayStateIndex.Property.ITEM));
                }
                List<ReplayFrame.BlockChange> blocks = frame.blocks();
                for (int i = blocks.size() - 1; i >= 0; i--) {
                    terrain.apply(blocks.get(i), true);
                    apply.accept(blocks.get(i), true);
                }
                index--;
            }
            states.restore(changed, index, frames, this::restore);
            last = index < 0 ? null : frames.get(index);
        }
        while (index + 1 < frames.size() && frames.get(index + 1).tick() <= target) {
            if (Thread.currentThread().isInterrupted()) throw new CancellationException();
            last = frames.get(++index);
            last.blocks().forEach(block -> { terrain.apply(block, false); apply.accept(block, false); });
            crossed.accept(last);
            remember(last);
        }
        if (last != null) {
            nextActors.clear();
            current = last;
            next = index + 1 < frames.size() ? frames.get(index + 1) : null;
            if (next != null) next.actors().forEach(actor -> nextActors.put(actor.id(), actor));
        }
        position = target;
        return last;
    }

    private ReplayFrame resetTo(double target, BiConsumer<ReplayFrame.BlockChange, Boolean> apply) {
        ReplayTimeline restored = new ReplayTimeline(frames, duration);
        restored.moveTo(target, (block, reverse) -> {});
        terrain.changesTo(restored.terrain).forEach(block -> apply.accept(block, false));
        states = restored.states;
        terrain = restored.terrain;
        originTick = -1;
        definitions.clear(); definitions.putAll(restored.definitions);
        equipment.clear(); equipment.putAll(restored.equipment);
        players.clear(); players.putAll(restored.players);
        items.clear(); items.putAll(restored.items);
        nextActors.clear(); nextActors.putAll(restored.nextActors);
        index = restored.index;
        position = restored.position;
        current = restored.current;
        next = restored.next;
        return current;
    }

    private void remember(ReplayFrame frame) {
        for (ReplayFrame.Actor actor : frame.actors()) {
            states.remember(index, actor);
            definitions.putIfAbsent(actor.id(), actor);
            if (actor.equipment() != null) equipment.put(actor.id(), actor.equipment());
            if (actor.player() != null) players.put(actor.id(), actor.player());
            if (actor.item() != null) items.put(actor.id(), actor.item());
        }
    }

    private void restore(ReplayStateIndex.Change change, ReplayFrame.Actor actor) {
        switch (change.property()) {
            case EQUIPMENT -> {
                if (actor == null) equipment.remove(change.actor());
                else equipment.put(change.actor(), actor.equipment());
            }
            case PLAYER -> {
                if (actor == null) players.remove(change.actor());
                else players.put(change.actor(), actor.player());
            }
            case ITEM -> {
                if (actor == null) items.remove(change.actor());
                else items.put(change.actor(), actor.item());
            }
        }
    }

    double position() { return position; }
    ReplayFrame.Actor definition(UUID actor) { return definitions.get(actor); }
    List<String> equipment(UUID actor) { return equipment.get(actor); }
    ReplayFrame.PlayerState player(UUID actor) { return players.get(actor); }
    String item(UUID actor) { return items.get(actor); }
    ReplayFrame currentFrame() { return current; }
    ReplayFrame.Actor nextActor(UUID actor) { return nextActors.get(actor); }
    double fraction() {
        return current == null || next == null ? 0 : (position - current.tick()) / (next.tick() - current.tick());
    }
    List<ReplayFrame.Actor> players() {
        return definitions.values().stream().filter(actor -> actor.type().equals("PLAYER"))
                .sorted(java.util.Comparator.comparing(ReplayFrame.Actor::name)).toList();
    }
}
