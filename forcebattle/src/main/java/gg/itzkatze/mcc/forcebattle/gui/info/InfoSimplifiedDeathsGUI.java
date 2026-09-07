package gg.itzkatze.mcc.forcebattle.gui.info;

import gg.itzkatze.mcc.data.DamageTypeSimplified;
import gg.itzkatze.mcc.data.MccDamageType;
import gg.itzkatze.mcc.gui.GUIClickableItem;
import gg.itzkatze.mcc.gui.GUIItem;
import gg.itzkatze.mcc.gui.GUIMaterial;
import gg.itzkatze.mcc.gui.MccGUI;
import gg.itzkatze.mcc.player.MccPlayer;
import gg.itzkatze.mcc.util.ItemStackCreator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

public class InfoSimplifiedDeathsGUI extends MccGUI {

    public InfoSimplifiedDeathsGUI() {
        super("Simplified Deaths Info", 27);
    }

    @Override
    public void setItems(final MccPlayer player) {
        fill(ItemStackCreator.fillerItem());
        set(GUIClickableItem.getGoBackItem(21, new InfoGUI()));
        set(GUIClickableItem.getCloseItem(22));

        int index = 0;
        for (DamageTypeSimplified simplified : DamageTypeSimplified.values()) {
            set(new GUIItem(index) {
                @Override
                public ItemStack getItem(final MccPlayer player) {
                    GUIMaterial guiMaterial = simplified.getGuiMaterial();
                    ArrayList<Component> lore = new ArrayList<>();

                    for (MccDamageType damageType : simplified.getDamageTypes()) {
                        lore.add(Component.text(" • " + damageType.getName()).color(NamedTextColor.WHITE));
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
