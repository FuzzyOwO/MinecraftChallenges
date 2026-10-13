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
            MccStructure.RUINED_PORTAL_OCEAN, MccStructure.RUINED_PORTAL_SWAMP, MccStructure.RUINED_PORTAL_NETHER, MccStructure.RUINED_PORTAL)),
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
    STRONGHOLD("Stronghold", new GUIMaterial(Material.END_PORTAL_FRAME), List.of(MccStructure.STRONGHOLD)),
    ABANDONED_CAMP("Abandoned Camp", new GUIMaterial(Material.CAMPFIRE), List.of(
            MccStructure.ABANDONED_CAMP_BAMBOO_JUNGLE, MccStructure.ABANDONED_CAMP_BIRCH_FOREST, MccStructure.ABANDONED_CAMP_CHERRY_GROVE,
            MccStructure.ABANDONED_CAMP_DAPPLED_FOREST, MccStructure.ABANDONED_CAMP_FLOWER_FOREST, MccStructure.ABANDONED_CAMP_FOREST,
            MccStructure.ABANDONED_CAMP_MEADOW, MccStructure.ABANDONED_CAMP_OLD_GROWTH_BIRCH_FOREST, MccStructure.ABANDONED_CAMP_OLD_GROWTH_PINE_TAIGA,
            MccStructure.ABANDONED_CAMP_OLD_GROWTH_SPRUCE_TAIGA, MccStructure.ABANDONED_CAMP_PALE_GARDEN, MccStructure.ABANDONED_CAMP_SAVANNA,
            MccStructure.ABANDONED_CAMP_SNOWY_TAIGA, MccStructure.ABANDONED_CAMP_SPARSE_JUNGLE, MccStructure.ABANDONED_CAMP_SWAMP,
            MccStructure.ABANDONED_CAMP_TAIGA, MccStructure.ABANDONED_CAMP_WINDSWEPT_FOREST, MccStructure.ABANDONED_CAMP_WOODED_BADLANDS));

    private final String name;
    private final GUIMaterial guiMaterial;
    private final List<MccStructure> structures;

    StructureSimplified(String name, GUIMaterial guiMaterial, List<MccStructure> structures) {
        this.name = name;
        this.guiMaterial = guiMaterial;
        this.structures = structures;
    }
}
