package gg.itzkatze.mcc.lobby.cosmetics;

import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

public enum PriceTier {

    COMMON(0, NamedTextColor.WHITE),
    UNCOMMON(60, NamedTextColor.GREEN),
    RARE(120, NamedTextColor.AQUA),
    EPIC(250, NamedTextColor.LIGHT_PURPLE),
    LEGENDARY(500, NamedTextColor.GOLD),
    ;

    private final int fromPrice;
    private final NamedTextColor color;

    PriceTier(int fromPrice, NamedTextColor color) {
        this.fromPrice = fromPrice;
        this.color = color;
    }

    public @NotNull NamedTextColor color() {
        return color;
    }

    public static @NotNull PriceTier of(int price) {
        PriceTier tier = COMMON;
        for (PriceTier candidate : values()) {
            if (price >= candidate.fromPrice) {
                tier = candidate;
            }
        }
        return tier;
    }
}
