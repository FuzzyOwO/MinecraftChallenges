package gg.itzkatze.mcc.data;

import gg.itzkatze.mcc.gui.GUIMaterial;
import lombok.Getter;
import org.bukkit.Material;

import java.util.List;

@Getter
public enum StructureSimplified {
    VILLAGE("Village", new GUIMaterial(Material.OAK_PLANKS), List.of(MccStructure.VILLAGE_PLAINS, MccStructure.VILLAGE_SAVANNA, MccStructure.VILLAGE_SNOWY, MccStructure.VILLAGE_TAIGA,
            MccStructure.VILLAGE_DESERT)),
    PILLAGER_OUTPOST("Pillager Outpost", new GUIMaterial(Material.DARK_OAK_LOG), List.of(MccStructure.PILLAGER_OUTPOST)),
    RUINED_PORTAL("Ruined Portal", new GUIMaterial(Material.CRYING_OBSIDIAN), List.of(
            MccStructure.RUINED_PORTAL_MOUNTAIN, MccStructure.RUINED_PORTAL_DESERT, MccStructure.RUINED_PORTAL_JUNGLE,
            MccStructure.RUINED_PORTAL_OCEAN, MccStructure.RUINED_PORTAL_SWAMP, MccStructure.RUINED_PORTAL_NETHER)),
    OCEAN_RUIN("Ocean Ruin", new GUIMaterial(Material.MOSSY_STONE_BRICKS), List.of(MccStructure.OCEAN_RUIN_COLD, MccStructure.OCEAN_RUIN_WARM)),
    TRIAL_CHAMBERS("Trial Chambers", new GUIMaterial(Material.WAXED_OXIDIZED_COPPER), List.of(MccStructure.TRIAL_CHAMBERS)),
    SHIPWRECK("Shipwreck", new GUIMaterial(Material.CHEST), List.of(MccStructure.SHIPWRECK, MccStructure.SHIPWRECK_BEACHED)),
    TRAIL_RUINS("Trail Ruins", new GUIMaterial(Material.GRAVEL), List.of(MccStructure.TRAIL_RUINS)),
    MANSION("Mansion", new GUIMaterial(Material.DARK_OAK_PLANKS), List.of(MccStructure.MANSION)),
    JUNGLE_PYRAMID("Jungle Pyramid", new GUIMaterial(Material.MOSSY_COBBLESTONE), List.of(MccStructure.JUNGLE_PYRAMID)),
    MINESHAFT("Mineshaft", new GUIMaterial(Material.RAIL), List.of(MccStructure.MINESHAFT, MccStructure.MINESHAFT_MESA)),
    SWAMP_HUT("Swamp Hut", new GUIMaterial(Material.CAULDRON), List.of(MccStructure.SWAMP_HUT)),
    END_CITY("End City", new GUIMaterial(Material.END_STONE_BRICKS), List.of(MccStructure.END_CITY)),
    ANCIENT_CITY("Ancient City", new GUIMaterial(Material.SCULK_SHRIEKER), List.of(MccStructure.ANCIENT_CITY)),
    DESERT_PYRAMID("Desert Pyramid", new GUIMaterial(Material.CHISELED_SANDSTONE), List.of(MccStructure.DESERT_PYRAMID)),
    MONUMENT("Monument", new GUIMaterial(Material.PRISMARINE), List.of(MccStructure.MONUMENT)),
    IGLOO("Igloo", new GUIMaterial(Material.SNOW_BLOCK), List.of(MccStructure.IGLOO)),
    BASTION_REMNANT("Bastion Remnant", new GUIMaterial(Material.POLISHED_BLACKSTONE_BRICKS), List.of(MccStructure.BASTION_REMNANT)),
    FORTRESS("Fortress", new GUIMaterial(Material.NETHER_BRICKS), List.of(MccStructure.FORTRESS)),
    STRONGHOLD("Stronghold", new GUIMaterial(Material.END_PORTAL_FRAME), List.of(MccStructure.STRONGHOLD));

    private final String name;
    private final GUIMaterial guiMaterial;
    private final List<MccStructure> structures;

    StructureSimplified(String name, GUIMaterial guiMaterial, List<MccStructure> structures) {
        this.name = name;
        this.guiMaterial = guiMaterial;
        this.structures = structures;
    }
}
