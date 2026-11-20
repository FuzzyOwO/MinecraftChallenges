package gg.itzkatze.katze.replay;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore;
import net.kyori.adventure.translation.GlobalTranslator;
import gg.itzkatze.katze.text.TextStyle;
import org.junit.jupiter.api.Test;

import java.util.AbstractList;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReplayCombatTest {
    @Test void localizedRecordingsWithoutAMarkerCanStillFindCombat() {
        var store = MiniMessageTranslationStore.create(Key.key("katzegames", "combat-test"), TextStyle.miniMessage());
        store.register("katze.mobarmy.phase.combat", Locale.ENGLISH, "Arena combat");
        store.register("katze.mobarmy.phase.combat", Locale.GERMAN, "Arenakampf");
        GlobalTranslator.translator().addSource(store);
        try {
            var first = frame(0, "katze.mobarmy.phase.configuration");
            var combat = frame(240, "katze.mobarmy.phase.combat");
            ReplayFrame.Actor original = combat.actors().getFirst();
            var state = new ReplayFrame.PlayerState(List.of(), null, null, 20, 20, 20, 0, "arena", List.of(),
                    Map.of("MOB_ARMY", List.of(ReplayText.encode(Component.translatable("katze.mobarmy.phase.combat")))));
            var actor = new ReplayFrame.Actor(original.id(), original.world(), "PLAYER", "Katze", null, null, null,
                    0, 64, 0, 0, 0, "STANDING", false, false, List.of(), null, false, 0, null, state);
            var frames = List.of(first, new ReplayFrame(240, List.of(actor), List.of(), List.of()));
            var replay = new ReplayStore.Loaded(new ReplayStore.Info(3, "26.3", UUID.randomUUID(), List.of(), 500), Map.of(), frames, first);
            assertEquals(240, ReplayCombat.find(replay));
        } finally {
            GlobalTranslator.translator().removeSource(store);
        }
    }

    @Test void recordedCombatMarkerDoesNotScanTheTimeline() {
        List<ReplayFrame> frames = new AbstractList<>() {
            @Override public ReplayFrame get(int index) { throw new AssertionError("Timeline scanned"); }
            @Override public int size() { return 100; }
        };
        var replay = new ReplayStore.Loaded(new ReplayStore.Info(3, "26.3", UUID.randomUUID(), List.of(), 1000, 740), Map.of(), frames, null);
        assertEquals(740, ReplayCombat.find(replay));
    }

    @Test void oldRecordingsFindTheFirstCombatFrameRatherThanConfiguration() {
        var frames = List.of(frame(0, "katze.mobarmy.phase.collection"), frame(100, "katze.mobarmy.phase.configuration"),
                frame(240, "katze.mobarmy.phase.combat"), frame(300, "katze.mobarmy.phase.combat"));
        var replay = new ReplayStore.Loaded(new ReplayStore.Info(3, "26.3", UUID.randomUUID(), List.of(), 500), Map.of(), frames, frames.getFirst());
        assertEquals(240, ReplayCombat.find(replay));
    }

    @Test void roundsWithoutCombatDoNotJumpToAnInventedTimestamp() {
        var frames = List.of(frame(0, "katze.mobarmy.phase.collection"));
        var replay = new ReplayStore.Loaded(new ReplayStore.Info(3, "26.3", UUID.randomUUID(), List.of(), 100), Map.of(), frames, frames.getFirst());
        assertEquals(-1, ReplayCombat.find(replay));
    }

    private ReplayFrame frame(int tick, String phase) {
        var state = new ReplayFrame.PlayerState(List.of(), null, null, 20, 20, 20, 0, "arena", List.of(),
                Map.of("MOB_ARMY", List.of(GsonComponentSerializer.gson().serialize(Component.translatable(phase)))));
        var actor = new ReplayFrame.Actor(UUID.randomUUID(), UUID.randomUUID(), "PLAYER", "Katze", null, null, null,
                0, 64, 0, 0, 0, "STANDING", false, false, List.of(), null, false, 0, null, state);
        return new ReplayFrame(tick, List.of(actor), List.of(), List.of());
    }
}
