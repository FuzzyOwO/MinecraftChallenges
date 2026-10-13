package gg.itzkatze.mcc.lobby.cosmetics;

import gg.itzkatze.mcc.gui.GUIClickableItem;
import gg.itzkatze.mcc.gui.GUIItem;
import gg.itzkatze.mcc.gui.MccGUI;
import gg.itzkatze.mcc.player.MccPlayer;
import gg.itzkatze.mcc.util.ItemStackCreator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class CosmeticConfirmGui extends MccGUI {

    private static final int ACCEPT_SLOT = 3;
    private static final int ITEM_SLOT = 4;
    private static final int DENY_SLOT = 5;

    private final Cosmetic cosmetic;
    private final MccGUI shop;

    public CosmeticConfirmGui(@NotNull Cosmetic cosmetic, @NotNull MccGUI shop) {
        super("Buy " + cosmetic.displayName() + "?", 9);
        this.cosmetic = cosmetic;
        this.shop = shop;
    }

    @Override
    public void setItems(MccPlayer player) {
        clear();
        fill(ItemStackCreator.fillerItem());
        set(new GUIClickableItem(ACCEPT_SLOT) {
            @Override
            public void run(InventoryClickEvent event, MccPlayer clicker) {
                accept(clicker);
            }

            @Override
            public ItemStack getItem(MccPlayer viewer) {
                return ItemStackCreator.getStack(Component.text("Accept", NamedTextColor.GREEN, TextDecoration.BOLD), Material.LIME_STAINED_GLASS_PANE,
                        List.of(Component.text("Buy " + cosmetic.displayName(), NamedTextColor.GRAY)));
            }
        });
        set(new GUIItem(ITEM_SLOT) {
            @Override
            public ItemStack getItem(MccPlayer viewer) {
                return summary(viewer);
            }
        });
        set(new GUIClickableItem(DENY_SLOT) {
            @Override
            public void run(InventoryClickEvent event, MccPlayer clicker) {
                deny(clicker);
            }

            @Override
            public ItemStack getItem(MccPlayer viewer) {
                return ItemStackCreator.getStack(Component.text("Deny", NamedTextColor.RED, TextDecoration.BOLD), Material.RED_STAINED_GLASS_PANE,
                        List.of(Component.text("Back to the shop", NamedTextColor.GRAY)));
            }
        });
    }

    @Override
    public boolean allowItemMoving() {
        return false;
    }

    private @NotNull ItemStack summary(@NotNull MccPlayer viewer) {
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

    private void accept(@NotNull MccPlayer clicker) {
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
        }
        shop.open(clicker);
    }

    private void deny(@NotNull MccPlayer clicker) {
        Player player = clicker.getPlayer();
        if (player != null) {
            player.playSound(ShopSounds.CLICK);
        }
        shop.open(clicker);
    }
}
