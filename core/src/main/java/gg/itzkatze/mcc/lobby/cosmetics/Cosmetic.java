package gg.itzkatze.mcc.lobby.cosmetics;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;

public interface Cosmetic {

    @NotNull String id();

    @NotNull CosmeticCategory category();

    @NotNull String displayName();

    int price();

    @NotNull Material icon();

    default @Nullable Season season() {
        return null;
    }

    default boolean isAvailable(@NotNull LocalDate today) {
        Season season = season();
        return season == null || season.isActive(today);
    }

    default @NotNull Component styledName() {
        return Component.text(displayName(), PriceTier.of(price()).color());
    }
}
