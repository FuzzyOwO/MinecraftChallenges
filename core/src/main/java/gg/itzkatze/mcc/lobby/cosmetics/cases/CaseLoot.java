package gg.itzkatze.mcc.lobby.cosmetics.cases;

import gg.itzkatze.mcc.lobby.cosmetics.Cosmetic;
import gg.itzkatze.mcc.lobby.cosmetics.CosmeticCatalog;
import gg.itzkatze.mcc.lobby.cosmetics.PriceTier;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class CaseLoot {

    public static final int PRICE = 100;

    // A row pays a fixed amount of points or, with points 0, a random available cosmetic of its rarity. Chances are in percent.
    private record Row(@NotNull PriceTier tier, double chance, int points) {
    }

    private static final List<Row> ROWS = List.of(
            new Row(PriceTier.COMMON, 25, 25), new Row(PriceTier.COMMON, 18, 50), new Row(PriceTier.COMMON, 15, 0),
            new Row(PriceTier.UNCOMMON, 10, 100), new Row(PriceTier.UNCOMMON, 12, 0),
            new Row(PriceTier.RARE, 6, 200), new Row(PriceTier.RARE, 8, 0),
            new Row(PriceTier.EPIC, 1.5, 500), new Row(PriceTier.EPIC, 2.5, 0),
            new Row(PriceTier.LEGENDARY, 0.4, 1000), new Row(PriceTier.LEGENDARY, 1.2, 0),
            new Row(PriceTier.MYTHIC, 0.4, 0));

    private CaseLoot() {
    }

    static @NotNull List<Cosmetic> available(@NotNull CosmeticCatalog catalog, @NotNull LocalDate today) {
        return catalog.all().stream().filter(cosmetic -> cosmetic.isAvailable(today)).toList();
    }

    static @NotNull CasePrize drawPrize(@NotNull List<? extends Cosmetic> available, @NotNull Random random) {
        return draw(available, random, false);
    }

    // The reel shows rare prizes far more often than they drop, so most rolls pass a few of them.
    static @NotNull CasePrize drawShown(@NotNull List<? extends Cosmetic> available, @NotNull Random random) {
        return draw(available, random, true);
    }

    static double boost(@NotNull PriceTier tier) {
        return switch (tier) {
            case COMMON -> 1;
            case UNCOMMON -> 1.5;
            case RARE -> 2.5;
            case EPIC -> 5;
            case LEGENDARY -> 10;
            case MYTHIC -> 25;
        };
    }

    private static @NotNull CasePrize draw(@NotNull List<? extends Cosmetic> available, @NotNull Random random, boolean boosted) {
        Map<PriceTier, List<Cosmetic>> pools = new EnumMap<>(PriceTier.class);
        for (Cosmetic cosmetic : available) {
            pools.computeIfAbsent(PriceTier.of(cosmetic.price()), tier -> new ArrayList<>()).add(cosmetic);
        }

        double[] weights = new double[ROWS.size()];
        double total = 0;
        for (int i = 0; i < ROWS.size(); i++) {
            Row row = ROWS.get(i);
            boolean empty = row.points() == 0 && !pools.containsKey(row.tier());
            weights[i] = empty ? 0 : row.chance() * (boosted ? boost(row.tier()) : 1);
            total += weights[i];
        }

        double roll = random.nextDouble() * total;
        int picked = -1;
        for (int i = 0; i < ROWS.size(); i++) {
            if (weights[i] <= 0) continue;
            picked = i;
            roll -= weights[i];
            if (roll < 0) break;
        }
        Row row = ROWS.get(picked);
        if (row.points() > 0) return new CasePrize.Points(row.points(), row.tier());

        List<Cosmetic> pool = pools.get(row.tier());
        return new CasePrize.Item(pool.get(random.nextInt(pool.size())));
    }
}
