package gg.itzkatze.katze.replay;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ReplayArchiveStressProbe {
    public static void main(String[] args) throws Exception {
        Path path = Path.of(args[0]);
        UUID world = UUID.randomUUID();
        var participants = new ArrayList<UUID>();
        for (int i = 0; i < 12; i++) participants.add(UUID.randomUUID());
        int duration = 90 * 60 * 20;
        var state = new ReplayFrame.PlayerState(List.of("inventory".repeat(1000)), List.of("backpack"), 1, 20, 20, 20, 15, "world",
                List.of("information"), Map.of("FORCEBATTLE", List.of("scoreboard")));
        try (var writer = new ReplayArchive.Writer(path, false)) {
            for (int tick = 0; tick < duration; tick++) {
                var actors = new ArrayList<ReplayFrame.Actor>();
                for (int i = 0; i < 12; i++) actors.add(new ReplayFrame.Actor(participants.get(i), world, "PLAYER", "player" + i,
                        null, null, null, tick * 0.1 + i, 64, i * 0.3, tick % 360, 0, "STANDING", false, false, null,
                        null, false, 0, null, tick % 20 == 0 ? state : null));
                writer.write(new ReplayFrame(tick, actors, List.of(), List.of(new ReplayFrame.Scene(world, "NORMAL", tick, false, false))));
            }
        }
        long started = System.nanoTime();
        var archive = ReplayArchive.open(path, new ReplayStore.Info(2, "26.3", UUID.randomUUID(), participants, duration));
        long indexedOpenMillis = (System.nanoTime() - started) / 1_000_000;
        if (archive.size() != duration) throw new AssertionError("Missing frames");
        var timeline = new ReplayTimeline(archive, duration);
        timeline.moveTo(duration - 1, (block, reverse) -> {});
        if (timeline.currentFrame().tick() != duration - 1) throw new AssertionError("Forward seek failed");
        timeline.moveTo(25, (block, reverse) -> {});
        if (!state.equals(timeline.player(participants.getFirst()))) throw new AssertionError("Backward seek lost player data");
        System.out.println("108000 frames, 12 players, 64 MiB heap; indexed open=" + indexedOpenMillis + " ms; archive bytes="
                + (Files.size(path) + Files.size(ReplayArchive.stringPath(path))));
    }
}
