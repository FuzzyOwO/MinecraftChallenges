package gg.itzkatze.mcc.lobby.jumpandrun;

import gg.itzkatze.mcc.lobby.cosmetics.PointsText;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Random;

final class EventBlock {

    static final Material MATERIAL = Material.GOLD_BLOCK;
    static final double CHANCE = 0.025;
    private static final int GOLD_FROM = 50;
    private static final int LEVEL_UP_FROM = 16;

    // Uniform within each range, chances in percent: an Event Block pays +6.9 points on average and takes some in 15% of the cases.
    private record Payout(int min, int max, double chance) {
    }

    private static final List<Payout> PAYOUTS = List.of(
            new Payout(-10, -3, 15),
            new Payout(1, 5, 50),
            new Payout(6, 15, 25),
            new Payout(16, 40, 8),
            new Payout(50, 100, 2));
    private static final double TOTAL = PAYOUTS.stream().mapToDouble(Payout::chance).sum();

    private static final Sound LOSS = Sound.sound(Key.key("entity.villager.no"), Sound.Source.PLAYER, 1f, 1f);
    private static final Sound WIN = Sound.sound(Key.key("block.note_block.pling"), Sound.Source.PLAYER, 1f, 2f);
    private static final Sound BIG_WIN = Sound.sound(Key.key("entity.player.levelup"), Sound.Source.PLAYER, 1f, 1f);

    private EventBlock() {
    }

    static boolean rolls(boolean afterEvent, @NotNull Random random) {
        return !afterEvent && random.nextDouble() < CHANCE;
    }

    static int payout(@NotNull Random random) {
        double roll = random.nextDouble() * TOTAL;
        Payout picked = PAYOUTS.getLast();
        for (Payout payout : PAYOUTS) {
            roll -= payout.chance();
            if (roll < 0) {
                picked = payout;
                break;
            }
        }
        return picked.min() + random.nextInt(picked.max() - picked.min() + 1);
    }

    static @NotNull Component message(int applied) {
        NamedTextColor color = applied < 0 ? NamedTextColor.RED : applied >= GOLD_FROM ? NamedTextColor.GOLD : NamedTextColor.GREEN;
        return Component.textOfChildren(Component.text("Event Block! ", NamedTextColor.GOLD, TextDecoration.BOLD), Component.text(PointsText.signed(applied), color));
    }

    static @NotNull Sound sound(int applied) {
        if (applied < 0) return LOSS;
        return applied >= LEVEL_UP_FROM ? BIG_WIN : WIN;
    }
}
