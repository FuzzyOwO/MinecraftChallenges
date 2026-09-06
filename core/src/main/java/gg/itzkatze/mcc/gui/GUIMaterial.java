package gg.itzkatze.mcc.gui;

import gg.itzkatze.mcc.util.Skull;
import org.bukkit.Material;

public record GUIMaterial(Material material, Skull skull) {
    public GUIMaterial(Material material) {
        this(material, null);
    }

    public GUIMaterial(Skull skull) {
        this(Material.PLAYER_HEAD, skull);
    }

    public boolean hasSkull() {
        return skull != null;
    }
}
