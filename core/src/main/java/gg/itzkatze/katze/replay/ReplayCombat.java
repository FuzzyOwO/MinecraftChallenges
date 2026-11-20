package gg.itzkatze.katze.replay;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CancellationException;

final class ReplayCombat {
    private static final String PHASE = "katze.mobarmy.phase.combat";

    private ReplayCombat() {}

    static int find(ReplayStore.Loaded replay) {
        Integer marker = replay.info().combatTick();
        if (marker != null && marker >= 0 && marker <= replay.info().duration()) return marker;
        String combat = PlainTextComponentSerializer.plainText().serialize(GlobalTranslator.render(Component.translatable(PHASE), Locale.ENGLISH));
        for (ReplayFrame frame : replay.frames()) {
            if (Thread.currentThread().isInterrupted()) throw new CancellationException();
            for (ReplayFrame.Actor actor : frame.actors()) {
                if (actor.player() == null) continue;
                for (String line : actor.player().scoreboards().getOrDefault("MOB_ARMY", List.of())) {
                    if (line.contains("\"" + PHASE + "\"") || PlainTextComponentSerializer.plainText()
                            .serialize(ReplayText.decode(line, Locale.ENGLISH)).equals(combat)) return frame.tick();
                }
            }
        }
        return -1;
    }
}
