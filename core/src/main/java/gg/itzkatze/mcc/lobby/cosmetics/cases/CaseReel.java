package gg.itzkatze.mcc.lobby.cosmetics.cases;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

final class CaseReel {

    static final int STEPS = 40;
    static final int WIDTH = 9;
    static final int LENGTH = STEPS + WIDTH;
    static final int PRIZE_INDEX = STEPS + WIDTH / 2;

    private final List<CasePrize> strip;

    private CaseReel(@NotNull List<CasePrize> strip) {
        this.strip = strip;
    }

    static @NotNull CaseReel build(@NotNull CasePrize prize, @NotNull Supplier<CasePrize> filler) {
        List<CasePrize> strip = new ArrayList<>(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            strip.add(i == PRIZE_INDEX ? prize : filler.get());
        }
        return new CaseReel(List.copyOf(strip));
    }

    @NotNull List<CasePrize> window(int position) {
        return strip.subList(position, position + WIDTH);
    }

    @NotNull CasePrize prize() {
        return strip.get(PRIZE_INDEX);
    }

    // The reel slows down towards the end: 19 one-tick steps, then longer and longer pauses, 90 ticks in all.
    static int delay(int step) {
        return 1 + (int) Math.round(5 * Math.pow(step / (double) (STEPS - 1), 3));
    }
}
