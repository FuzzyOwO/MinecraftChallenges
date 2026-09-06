package gg.itzkatze.mcc.forcebattle.gui.info;

import gg.itzkatze.mcc.data.MobsSimplified;
import gg.itzkatze.mcc.gui.GUIItem;
import gg.itzkatze.mcc.gui.GUIMaterial;
import gg.itzkatze.mcc.gui.MccGUI;
import gg.itzkatze.mcc.player.MccPlayer;
import gg.itzkatze.mcc.util.ItemStackCreator;
import gg.itzkatze.mcc.util.StringUtility;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

public class InfoSimplifiedMobsGUI extends MccGUI {

    public InfoSimplifiedMobsGUI() {
        super("Simplified Mobs Info", 27);
    }

    @Override
    public void setItems(final MccPlayer player) {
        int index = 0;
        for (MobsSimplified simplified : MobsSimplified.values()) {
            set(new GUIItem(index) {
                @Override
                public ItemStack getItem(final MccPlayer player) {
                    GUIMaterial guiMaterial = simplified.getGuiMaterial();
                    ArrayList<Component> lore = new ArrayList<>();

                    for (EntityType entityType : simplified.getMobs()) {
                        lore.add(Component.text(" • " + StringUtility.formatName(entityType.key().value())).color(NamedTextColor.WHITE));
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
