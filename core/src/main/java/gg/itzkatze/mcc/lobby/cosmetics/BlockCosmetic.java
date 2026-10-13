package gg.itzkatze.mcc.lobby.cosmetics;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

public enum BlockCosmetic implements Cosmetic {

    BRICKS("bricks", Material.BRICKS, "Bricks", 30),
    STONE_BRICKS("stone_bricks", Material.STONE_BRICKS, "Stone Bricks", 30),
    MOSSY_STONE_BRICKS("mossy_stone_bricks", Material.MOSSY_STONE_BRICKS, "Mossy Stone Bricks", 30),
    CHERRY_PLANKS("cherry_planks", Material.CHERRY_PLANKS, "Cherry Planks", 30),
    BONE_BLOCK("bone_block", Material.BONE_BLOCK, "Bone Block", 30),
    HAY_BALE("hay_bale", Material.HAY_BLOCK, "Hay Bale", 30),
    PRISMARINE_BRICKS("prismarine_bricks", Material.PRISMARINE_BRICKS, "Prismarine Bricks", 60),
    PURPUR_BLOCK("purpur_block", Material.PURPUR_BLOCK, "Purpur Block", 60),
    QUARTZ_BLOCK("quartz_block", Material.QUARTZ_BLOCK, "Quartz Block", 60),
    LAPIS_BLOCK("lapis_block", Material.LAPIS_BLOCK, "Lapis Block", 80),
    IRON_BLOCK("iron_block", Material.IRON_BLOCK, "Iron Block", 80),
    WAXED_COPPER_BLOCK("waxed_copper_block", Material.WAXED_COPPER_BLOCK, "Waxed Copper Block", 80),
    HONEYCOMB_BLOCK("honeycomb_block", Material.HONEYCOMB_BLOCK, "Honeycomb Block", 100),
    OBSIDIAN("obsidian", Material.OBSIDIAN, "Obsidian", 100),
    GLOWSTONE("glowstone", Material.GLOWSTONE, "Glowstone", 120),
    SEA_LANTERN("sea_lantern", Material.SEA_LANTERN, "Sea Lantern", 120),
    SHROOMLIGHT("shroomlight", Material.SHROOMLIGHT, "Shroomlight", 120),
    AMETHYST_BLOCK("amethyst_block", Material.AMETHYST_BLOCK, "Amethyst Block", 150),
    GOLD_BLOCK("gold_block", Material.GOLD_BLOCK, "Gold Block", 150),
    CRYING_OBSIDIAN("crying_obsidian", Material.CRYING_OBSIDIAN, "Crying Obsidian", 150),
    VERDANT_FROGLIGHT("verdant_froglight", Material.VERDANT_FROGLIGHT, "Verdant Froglight", 180),
    OCHRE_FROGLIGHT("ochre_froglight", Material.OCHRE_FROGLIGHT, "Ochre Froglight", 180),
    PEARLESCENT_FROGLIGHT("pearlescent_froglight", Material.PEARLESCENT_FROGLIGHT, "Pearlescent Froglight", 180),
    EMERALD_BLOCK("emerald_block", Material.EMERALD_BLOCK, "Emerald Block", 200),
    DIAMOND_BLOCK("diamond_block", Material.DIAMOND_BLOCK, "Diamond Block", 250),
    ANCIENT_DEBRIS("ancient_debris", Material.ANCIENT_DEBRIS, "Ancient Debris", 350),
    NETHERITE_BLOCK("netherite_block", Material.NETHERITE_BLOCK, "Netherite Block", 500);

    private final String id;
    private final Material material;
    private final String displayName;
    private final int price;

    BlockCosmetic(String id, Material material, String displayName, int price) {
        this.id = id;
        this.material = material;
        this.displayName = displayName;
        this.price = price;
    }

    @Override
    public @NotNull String id() {
        return id;
    }

    @Override
    public @NotNull CosmeticCategory category() {
        return CosmeticCategory.BLOCK;
    }

    @Override
    public @NotNull String displayName() {
        return displayName;
    }

    @Override
    public int price() {
        return price;
    }

    @Override
    public @NotNull Material icon() {
        return material;
    }

    public @NotNull Material material() {
        return material;
    }
}
