package gg.itzkatze.mcc.gui.sign;

import gg.itzkatze.mcc.player.MccPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.function.BiConsumer;

public final class NumberSignInput {

    private NumberSignInput() {
    }

    public static void open(
            MccPlayer player,
            String label,
            int current,
            int min,
            int max,
            BiConsumer<MccPlayer, Integer> onComplete,
            BiConsumer<MccPlayer, String> onError
    ) {
        SignInput.open(
                player,
                List.of(
                        Component.text(label).color(NamedTextColor.AQUA),
                        Component.text("Current: " + current).color(NamedTextColor.GRAY),
                        Component.text("Min " + min + " Max " + max).color(NamedTextColor.YELLOW),
                        Component.empty()
                ),
                (mccPlayer, lines) -> {
                    String raw = lines.isEmpty() ? "" : lines.getFirst().trim();
                    if (raw.isEmpty()) {
                        if (onError != null) {
                            onError.accept(mccPlayer, "No value entered.");
                        }
                        return;
                    }
                    int value;
                    try {
                        value = Integer.parseInt(raw);
                    } catch (NumberFormatException e) {
                        if (onError != null) {
                            onError.accept(mccPlayer, "Invalid number.");
                        }
                        return;
                    }

                    int clamped = Math.max(min, Math.min(max, value));
                    if (value != clamped && onError != null) {
                        onError.accept(mccPlayer, "Clamped to " + clamped + ".");
                    }
                    onComplete.accept(mccPlayer, clamped);
                }
        );
    }
}
