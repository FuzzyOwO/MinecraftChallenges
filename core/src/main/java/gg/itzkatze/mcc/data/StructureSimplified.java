package gg.itzkatze.mcc.data;

import lombok.Getter;

import java.util.List;

@Getter
public enum StructureSimplified {
    VILLAGE("Village", List.of(MccStructure.VILLAGE_PLAINS, MccStructure.VILLAGE_SAVANNA, MccStructure.VILLAGE_SNOWY, MccStructure.VILLAGE_TAIGA, MccStructure.VILLAGE_DESERT)),
    PILLAGER_OUTPOST("Pillager Outpost", List.of(MccStructure.PILLAGER_OUTPOST)),
    RUINED_PORTAL("Ruined Portal", List.of(
            MccStructure.RUINED_PORTAL_MOUNTAIN, MccStructure.RUINED_PORTAL_DESERT, MccStructure.RUINED_PORTAL_JUNGLE,
            MccStructure.RUINED_PORTAL_OCEAN, MccStructure.RUINED_PORTAL_SWAMP, MccStructure.RUINED_PORTAL_NETHER)),
    OCEAN_RUIN("Ocean Ruin", List.of(MccStructure.OCEAN_RUIN_COLD, MccStructure.OCEAN_RUIN_WARM)),
    TRIAL_CHAMBERS("Trial Chambers", List.of(MccStructure.TRIAL_CHAMBERS)),
    SHIPWRECK("Shipwreck", List.of(MccStructure.SHIPWRECK, MccStructure.SHIPWRECK_BEACHED)),
    TRAIL_RUINS("Trail Ruins", List.of(MccStructure.TRAIL_RUINS)),
    MANSION("Mansion", List.of(MccStructure.MANSION)),
    JUNGLE_PYRAMID("Jungle Pyramid", List.of(MccStructure.JUNGLE_PYRAMID)),
    MINESHAFT("Mineshaft", List.of(MccStructure.MINESHAFT, MccStructure.MINESHAFT_MESA)),
    SWAMP_HUT("Swamp Hut", List.of(MccStructure.SWAMP_HUT)),
    END_CITY("End City", List.of(MccStructure.END_CITY)),
    ANCIENT_CITY("Ancient City", List.of(MccStructure.ANCIENT_CITY)),
    DESERT_PYRAMID("Desert Pyramid", List.of(MccStructure.DESERT_PYRAMID)),
    MONUMENT("Monument", List.of(MccStructure.MONUMENT)),
    IGLOO("Igloo", List.of(MccStructure.IGLOO)),
    BASTION_REMNANT("Bastion Remnant", List.of(MccStructure.BASTION_REMNANT)),
    FORTRESS("Fortress", List.of(MccStructure.FORTRESS)),
    STRONGHOLD("Stronghold", List.of(MccStructure.STRONGHOLD));

    private final String name;
    private final List<MccStructure> structures;

    StructureSimplified(String name, List<MccStructure> structures) {
        this.name = name;
        this.structures = structures;
    }
}
