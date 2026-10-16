package gg.itzkatze.mcc.lobby.cosmetics.cases;

import gg.itzkatze.mcc.lobby.cosmetics.Cosmetic;
import gg.itzkatze.mcc.lobby.cosmetics.PriceTier;
import org.jetbrains.annotations.NotNull;

public sealed interface CasePrize {

    @NotNull PriceTier tier();

    record Points(int amount, @NotNull PriceTier tier) implements CasePrize {
    }

    record Item(@NotNull Cosmetic cosmetic) implements CasePrize {

        @Override
        public @NotNull PriceTier tier() {
            return PriceTier.of(cosmetic.price());
        }
    }
}
