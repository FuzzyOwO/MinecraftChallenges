package gg.itzkatze.mcc.lobby.cosmetics;

import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

public enum PriceTier {

    COMMON(0, NamedTextColor.WHITE, "Common"),
    UNCOMMON(100, NamedTextColor.GREEN, "Uncommon"),
    RARE(200, NamedTextColor.BLUE, "Rare"),
    EPIC(500, NamedTextColor.DARK_PURPLE, "Epic"),
    LEGENDARY(1000, NamedTextColor.GOLD, "Legendary"),
    MYTHIC(2000, NamedTextColor.LIGHT_PURPLE, "Mythic"),
    ;

    private final int fromPrice;
    private final NamedTextColor color;
    private final String displayName;

    PriceTier(int fromPrice, NamedTextColor color, String displayName) {
        this.fromPrice = fromPrice;
        this.color = color;
        this.displayName = displayName;
    }

    public @NotNull NamedTextColor color() {
        return color;
    }

    public @NotNull String displayName() {
        return displayName;
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
