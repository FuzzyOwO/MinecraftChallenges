package gg.itzkatze.mcc.lobby.cosmetics;

import gg.itzkatze.mcc.gui.MccGUI;
import gg.itzkatze.mcc.player.MccPlayer;
import gg.itzkatze.mcc.util.ItemStackCreator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class CosmeticConfirmGui extends PurchaseConfirmGui {

    private final Cosmetic cosmetic;
    private final MccGUI shop;

    public CosmeticConfirmGui(@NotNull Cosmetic cosmetic, @NotNull MccGUI shop) {
        super("Buy " + cosmetic.displayName() + "?");
        this.cosmetic = cosmetic;
        this.shop = shop;
    }

    @Override
    protected @NotNull String acceptHint() {
        return "Buy " + cosmetic.displayName();
    }

    @Override
    protected @NotNull String denyHint() {
        return "Back to the shop";
    }

    @Override
    protected @NotNull ItemStack summary(@NotNull MccPlayer viewer) {
        CosmeticStore store = CosmeticStore.get();
        int balance = store.points(viewer.getUuid());
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Price: " + PointsText.amount(cosmetic.price()), NamedTextColor.GOLD));
        if (store.owns(viewer.getUuid(), cosmetic)) {
            lore.add(Component.text("You already own this.", NamedTextColor.RED));
        } else if (balance < cosmetic.price()) {
            lore.add(Component.text(PointsText.missing(cosmetic.price() - balance), NamedTextColor.RED));
        } else {
            lore.add(Component.text("After buying: " + PointsText.amount(balance - cosmetic.price()), NamedTextColor.GRAY));
        }
        return ItemStackCreator.getStack(cosmetic.styledName(), cosmetic.icon(), lore);
    }

    @Override
    protected void accept(@NotNull MccPlayer clicker) {
        Player player = clicker.getPlayer();
        if (player == null) return;

        CosmeticStore store = CosmeticStore.get();
        switch (store.buy(player, cosmetic)) {
            case BOUGHT -> {
                player.sendMessage(Component.text("Bought ", NamedTextColor.GREEN)
                        .append(cosmetic.styledName())
                        .append(Component.text(" for " + PointsText.amount(cosmetic.price()) + ".", NamedTextColor.GREEN)));
                player.playSound(ShopSounds.CONFIRM);
            }
            case ALREADY_OWNED -> {
                player.sendMessage(Component.text("You already own ", NamedTextColor.RED)
                        .append(cosmetic.styledName())
                        .append(Component.text(".", NamedTextColor.RED)));
                player.playSound(ShopSounds.DENY);
            }
            case NOT_ENOUGH_POINTS -> {
                player.sendMessage(Component.text(PointsText.missing(cosmetic.price() - store.points(player.getUniqueId())), NamedTextColor.RED));
                player.playSound(ShopSounds.DENY);
            }
            case OUT_OF_SEASON -> {
                Season season = cosmetic.season();
                if (season != null) {
                    player.sendMessage(season.returnsMessage(cosmetic));
                }
                player.playSound(ShopSounds.DENY);
            }
        }
        shop.open(clicker);
    }

    @Override
    protected void deny(@NotNull MccPlayer clicker) {
        Player player = clicker.getPlayer();
        if (player != null) {
            player.playSound(ShopSounds.CLICK);
        }
        shop.open(clicker);
    }
}
