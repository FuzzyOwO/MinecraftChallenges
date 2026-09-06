package gg.itzkatze.mcc.forcebattle.gui.info;

import gg.itzkatze.mcc.data.MccStructure;
import gg.itzkatze.mcc.data.StructureSimplified;
import gg.itzkatze.mcc.gui.GUIItem;
import gg.itzkatze.mcc.gui.GUIMaterial;
import gg.itzkatze.mcc.gui.MccGUI;
import gg.itzkatze.mcc.player.MccPlayer;
import gg.itzkatze.mcc.util.ItemStackCreator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

public class InfoSimplifiedStructuresGUI extends MccGUI {

    public InfoSimplifiedStructuresGUI() {
        super("Simplified Structures Info", 27);
    }

    @Override
    public void setItems(final MccPlayer player) {
        int index = 0;
        for (StructureSimplified simplified : StructureSimplified.values()) {
            set(new GUIItem(index) {
                @Override
                public ItemStack getItem(final MccPlayer player) {
                    GUIMaterial guiMaterial = simplified.getGuiMaterial();
                    ArrayList<Component> lore = new ArrayList<>();

                    for (MccStructure biome : simplified.getStructures()) {
                        lore.add(Component.text(" • " + biome.getName()).color(NamedTextColor.WHITE));
                    }

                    if (guiMaterial.hasSkull()) {
                        return ItemStackCreator.getSkull(guiMaterial.skull(), Component.text(simplified.getName()).color(NamedTextColor.GOLD), 1, lore);
                    } else {
                        return ItemStackCreator.getStack(Component.text(simplified.getName()).color(NamedTextColor.GOLD), guiMaterial.material(), 1, lore);
                    }
                }
            });

            index++;
        }
    }

    @Override
    public boolean allowItemMoving() {
        return false;
    }

}
