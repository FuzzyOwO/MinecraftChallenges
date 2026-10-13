package gg.itzkatze.mcc.lobby.cosmetics;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

public interface Cosmetic {

    @NotNull String id();

    @NotNull CosmeticCategory category();

    @NotNull String displayName();

    int price();

    @NotNull Material icon();

    default @NotNull Component styledName() {
        return Component.text(displayName(), PriceTier.of(price()).color());
    }
}
