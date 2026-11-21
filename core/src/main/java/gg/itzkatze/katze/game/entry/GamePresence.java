package gg.itzkatze.katze.game.entry;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GamePresence {
    public enum State { LIMBO, PLAYING, VIEWING, PENDING }

    private final UUID roundId;
    private final Map<UUID, Entry> entries = new HashMap<>();

    public GamePresence(UUID roundId) {
        this.roundId = roundId;
    }

    public UUID roundId() {
        return roundId;
    }

    public long generation(UUID playerId) {
        Entry entry = entries.get(playerId);
        return entry == null ? 0 : entry.generation();
    }

    public State state(UUID playerId) {
        Entry entry = entries.get(playerId);
        return entry == null ? State.LIMBO : entry.state();
    }

    public boolean transition(UUID playerId, long expectedGeneration, State target) {
        if (generation(playerId) != expectedGeneration) return false;
        entries.put(playerId, new Entry(target, expectedGeneration + 1));
        return true;
    }

    public boolean isPlaying(UUID playerId) {
        return state(playerId) == State.PLAYING;
    }

    private record Entry(State state, long generation) {}
}
