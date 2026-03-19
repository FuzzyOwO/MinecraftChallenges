package gg.itzkatze.mcc.allitems.gui;

import gg.itzkatze.mcc.allitems.AllItemsObjectiveType;
import gg.itzkatze.mcc.allitems.AllItemsSettings;
import gg.itzkatze.mcc.gui.GUIClickableItem;
import gg.itzkatze.mcc.gui.MccGUI;
import gg.itzkatze.mcc.player.MccPlayer;
import gg.itzkatze.mcc.util.ItemStackCreator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class AllItemsMissingObjectivesMenu extends MccGUI {

    public AllItemsMissingObjectivesMenu() {
        super("Missing Objectives", 27);
    }

    @Override
    public void setItems(MccPlayer player) {
        fill(ItemStackCreator.fillerItem());

        setTypeButton(10, AllItemsObjectiveType.ITEM, AllItemsSettings.isItemsEnabled());
        setTypeButton(11, AllItemsObjectiveType.BIOME, AllItemsSettings.isBiomesEnabled());
        setTypeButton(12, AllItemsObjectiveType.MOB, AllItemsSettings.isMobsEnabled());
        setTypeButton(13, AllItemsObjectiveType.STRUCTURE, AllItemsSettings.isStructuresEnabled());
        setTypeButton(14, AllItemsObjectiveType.ADVANCEMENT, AllItemsSettings.isAdvancementsEnabled());

        set(GUIClickableItem.getGoBackItem(18, new AllItemsSettingsGUI()));
    }

    private void setTypeButton(int slot, AllItemsObjectiveType type, boolean enabled) {
        set(new GUIClickableItem(slot) {
            @Override
            public void run(InventoryClickEvent event, MccPlayer player) {
                new AllItemsMissingObjectivesListGUI(type, 0).open(player);
            }

            @Override
            public ItemStack getItem(MccPlayer p) {
                NamedTextColor color = enabled ? NamedTextColor.AQUA : NamedTextColor.RED;
                return ItemStackCreator.getStack(
                        Component.text(type.getDisplayName()).color(color),
                        type.getIcon(),
                        1,
                        List.of(
                                Component.text("Status: ").color(NamedTextColor.GRAY)
                                        .append(Component.text(enabled ? "Enabled" : "Disabled").color(color)),
                                Component.empty(),
                                Component.text("Click to view").color(NamedTextColor.YELLOW)
                        )
                );
            }
        });
    }

    @Override
    public boolean allowItemMoving() {
        return false;
    }
}
