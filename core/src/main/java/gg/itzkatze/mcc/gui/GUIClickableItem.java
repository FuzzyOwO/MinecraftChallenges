package gg.itzkatze.mcc.gui;

import gg.itzkatze.mcc.player.MccPlayer;
import gg.itzkatze.mcc.util.ItemStackCreator;
import gg.itzkatze.mcc.util.Skull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public abstract class GUIClickableItem extends GUIItem {

    public GUIClickableItem(final int slot) {
        super(slot);
    }

    public abstract void run(InventoryClickEvent event, MccPlayer player);

    public static GUIClickableItem getCloseItem(int slot) {
        return new GUIClickableItem(slot) {
            @Override
            public void run(InventoryClickEvent e, MccPlayer player) {
                if (player.getPlayer() == null) return;
                player.getPlayer().closeInventory();
            }

            @Override
            public ItemStack getItem(MccPlayer player) {
                return ItemStackCreator.createNamedItemStack(
                        Material.BARRIER,
                        Component.text("Close").color(NamedTextColor.RED)
                );
            }
        };
    }

    public static GUIClickableItem getGoBackItem(int slot, MccGUI gui) {
        return new GUIClickableItem(slot) {
            @Override
            public void run(InventoryClickEvent e, MccPlayer player) {
                gui.open(player);
            }

            @Override
            public ItemStack getItem(MccPlayer player) {
                return ItemStackCreator.getSkull(
                        Skull.ARROW_LEFT,
                        Component.text("Go Back").color(NamedTextColor.GREEN),
                        1,
                        Component.text("To " + gui.getTitle()).color(NamedTextColor.GRAY)
                );
            }
        };
    }

    public static GUIClickableItem getGoForthItem(int slot, MccGUI gui) {
        return new GUIClickableItem(slot) {
            @Override
            public void run(InventoryClickEvent e, MccPlayer player) {
                gui.open(player);
            }

            @Override
            public ItemStack getItem(MccPlayer player) {
                return ItemStackCreator.getSkull(
                        Skull.ARROW_RIGHT,
                        Component.text("Go Forth").color(NamedTextColor.GREEN),
                        1,
                        Component.text("To " + gui.getTitle()).color(NamedTextColor.GRAY)
                );
            }
        };
    }

    @Override
    public void onClick(InventoryClickEvent event, MccPlayer player) {
        run(event, player);
    }

}
