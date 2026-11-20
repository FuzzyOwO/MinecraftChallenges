package gg.itzkatze.katze.replay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.BiConsumer;

final class ReplayStateIndex {
    enum Property { EQUIPMENT, PLAYER, ITEM }

    record Change(UUID actor, Property property) {}

    private static final class History {
        private final int first;
        private final Map<Property, List<Integer>> updates = new EnumMap<>(Property.class);

        private History(int first) { this.first = first; }

        private void add(Property property, int frame) {
            List<Integer> versions = updates.computeIfAbsent(property, ignored -> new ArrayList<>());
            if (versions.isEmpty() || versions.getLast() < frame) versions.add(frame);
        }

        private int at(Property property, int frame) {
            List<Integer> versions = updates.get(property);
            if (versions == null) return -1;
            int index = Collections.binarySearch(versions, frame);
            if (index < 0) index = -index - 2;
            return index < 0 ? -1 : versions.get(index);
        }
    }

    private final Map<UUID, History> actors = new HashMap<>();

    void remember(int frame, ReplayFrame.Actor actor) {
        History history = actors.computeIfAbsent(actor.id(), ignored -> new History(frame));
        if (actor.equipment() != null) history.add(Property.EQUIPMENT, frame);
        if (actor.player() != null) history.add(Property.PLAYER, frame);
        if (actor.item() != null) history.add(Property.ITEM, frame);
    }

    boolean firstAppearance(UUID actor, int frame) { return actors.get(actor).first == frame; }

    void restore(Set<Change> changes, int target, List<ReplayFrame> frames, BiConsumer<Change, ReplayFrame.Actor> restore) {
        Map<Integer, Map<UUID, List<Change>>> required = new TreeMap<>();
        for (Change change : changes) {
            int version = actors.get(change.actor()).at(change.property(), target);
            if (version < 0) restore.accept(change, null);
            else required.computeIfAbsent(version, ignored -> new HashMap<>())
                    .computeIfAbsent(change.actor(), ignored -> new ArrayList<>()).add(change);
        }
        for (var entry : required.entrySet()) {
            for (ReplayFrame.Actor actor : frames.get(entry.getKey()).actors()) {
                List<Change> updates = entry.getValue().get(actor.id());
                if (updates != null) updates.forEach(change -> restore.accept(change, actor));
            }
        }
    }
}
