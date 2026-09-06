package gg.itzkatze.mcc.forcebattle.gui.info;

import gg.itzkatze.mcc.gui.GUIClickableItem;
import gg.itzkatze.mcc.gui.MccGUI;
import gg.itzkatze.mcc.player.MccPlayer;
import gg.itzkatze.mcc.util.ItemStackCreator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class InfoGUI extends MccGUI {

    public InfoGUI() {
        super("Information", 27);
    }

    @Override
    public void setItems(final MccPlayer player) {
        fill(ItemStackCreator.fillerItem());

        set(new GUIClickableItem(11) {
            @Override
            public ItemStack getItem(final MccPlayer player) {
                return ItemStackCreator.getStack(Component.text("Simplified Biomes").color(NamedTextColor.BLUE), Material.GRASS_BLOCK, 1);
            }

            @Override
            public void run(final InventoryClickEvent event, final MccPlayer player) {
                new InfoSimplifiedBiomesGUI().open(player);
            }
        });

        set(new GUIClickableItem(13) {
            @Override
            public ItemStack getItem(final MccPlayer player) {
                return ItemStackCreator.getStack(Component.text("Simplified Structures").color(NamedTextColor.BLUE), Material.STRUCTURE_BLOCK, 1);
            }

            @Override
            public void run(final InventoryClickEvent event, final MccPlayer player) {
                new InfoSimplifiedStructuresGUI().open(player);
            }
        });

        set(new GUIClickableItem(15) {
            @Override
            public ItemStack getItem(final MccPlayer player) {
                return ItemStackCreator.getStack(Component.text("Simplified Mobs").color(NamedTextColor.BLUE), Material.DIAMOND_SWORD, 1);
            }

            @Override
            public void run(final InventoryClickEvent event, final MccPlayer player) {
                new InfoSimplifiedMobsGUI().open(player);
            }
        });
    }

    @Override
    public boolean allowItemMoving() {
        return false;
    }

}
