package gg.itzkatze.mcc.gui;

import gg.itzkatze.mcc.player.MccPlayer;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public abstract class GUIItem {
    public final int slot;
    public abstract ItemStack getItem(MccPlayer player);

    public GUIItem(int slot) {
        this.slot = slot;
    }

    public void onClick(InventoryClickEvent event, MccPlayer player) {
    }
}
