package gg.itzkatze.mcc.lobby.cosmetics;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum BlockCosmetic implements Cosmetic {

    BRICKS("bricks", Material.BRICKS, "Bricks", 30),
    CHERRY_PLANKS("cherry_planks", Material.CHERRY_PLANKS, "Cherry Planks", 50),
    PALE_OAK_PLANKS("pale_oak_planks", Material.PALE_OAK_PLANKS, "Pale Oak Planks", 50),
    BAMBOO_PLANKS("bamboo_planks", Material.BAMBOO_PLANKS, "Bamboo Planks", 50),
    PURPUR_BLOCK("purpur_block", Material.PURPUR_BLOCK, "Purpur Block", 75),
    QUARTZ_BLOCK("quartz_block", Material.QUARTZ_BLOCK, "Quartz Block", 75),
    TUFF_BRICKS("tuff_bricks", Material.TUFF_BRICKS, "Tuff Bricks", 100),
    MAGMA_BLOCK("magma_block", Material.MAGMA_BLOCK, "Magma Block", 125),
    SEA_LANTERN("sea_lantern", Material.SEA_LANTERN, "Sea Lantern", 150),
    SHROOMLIGHT("shroomlight", Material.SHROOMLIGHT, "Shroomlight", 200),
    WAXED_BLOCK_OF_COPPER("waxed_block_of_copper", Material.WAXED_COPPER_BLOCK, "Waxed Copper Block", 200),
    WAXED_CUT_COPPER("waxed_cut_copper", Material.WAXED_CUT_COPPER, "Waxed Cut Copper", 200),
    RED_MUSHROOM_BLOCK("red_mushroom_block", Material.RED_MUSHROOM_BLOCK, "Red Mushroom Block", 250),
    BROWN_MUSHROOM_BLOCK("brown_mushroom_block", Material.BROWN_MUSHROOM_BLOCK, "Brown Mushroom Block", 250),
    TUBE_CORAL_BLOCK("tube_coral_block", Material.TUBE_CORAL_BLOCK, "Tube Coral Block", 250),
    BRAIN_CORAL_BLOCK("brain_coral_block", Material.BRAIN_CORAL_BLOCK, "Brain Coral Block", 250),
    BUBBLE_CORAL_BLOCK("bubble_coral_block", Material.BUBBLE_CORAL_BLOCK, "Bubble Coral Block", 250),
    FIRE_CORAL_BLOCK("fire_coral_block", Material.FIRE_CORAL_BLOCK, "Fire Coral Block", 250),
    HORN_CORAL_BLOCK("horn_coral_block", Material.HORN_CORAL_BLOCK, "Horn Coral Block", 250),
    AMETHYST_BLOCK("amethyst_block", Material.AMETHYST_BLOCK, "Amethyst Block", 300),
    EMERALD_ORE("emerald_ore", Material.EMERALD_ORE, "Emerald Ore", 300),
    REDSTONE_ORE("redstone_ore", Material.REDSTONE_ORE, "Redstone Ore", 300),
    GOLD_ORE("gold_ore", Material.GOLD_ORE, "Gold Ore", 300),
    DIAMOND_ORE("diamond_ore", Material.DIAMOND_ORE, "Diamond Ore", 300),
    BLOCK_OF_RAW_GOLD("block_of_raw_gold", Material.RAW_GOLD_BLOCK, "Block Of Raw Gold", 400),
    BLOCK_OF_RESIN("block_of_resin", Material.RESIN_BLOCK, "Block Of Resin", 400),
    GILDED_BLACKSTONE("gilded_blackstone", Material.GILDED_BLACKSTONE, "Gilded Blackstone", 400),
    NETHERITE_BLOCK("netherite_block", Material.NETHERITE_BLOCK, "Netherite Block", 500),
    HONEYCOMB_BLOCK("honeycomb_block", Material.HONEYCOMB_BLOCK, "Honeycomb Block", 500),
    VERDANT_FROGLIGHT("verdant_froglight", Material.VERDANT_FROGLIGHT, "Verdant Froglight", 1000),
    OCHRE_FROGLIGHT("ochre_froglight", Material.OCHRE_FROGLIGHT, "Ochre Froglight", 1000),
    PEARLESCENT_FROGLIGHT("pearlescent_froglight", Material.PEARLESCENT_FROGLIGHT, "Pearlescent Froglight", 1000),
    BEACON("beacon", Material.BEACON, "Beacon", 2000),

    JACK_O_LANTERN("jack_o_lantern", Material.JACK_O_LANTERN, "Jack o'Lantern", 300, Season.HALLOWEEN),
    FRESH_SNOW("fresh_snow", Material.SNOW_BLOCK, "Fresh Snow", 300, Season.CHRISTMAS),
    MELON("melon", Material.MELON, "Melon", 300, Season.SUMMER),
    ;

    private final String id;
    private final Material material;
    private final String displayName;
    private final int price;
    private final @Nullable Season season;

    BlockCosmetic(String id, Material material, String displayName, int price) {
        this(id, material, displayName, price, null);
    }

    BlockCosmetic(String id, Material material, String displayName, int price, @Nullable Season season) {
        this.id = id;
        this.material = material;
        this.displayName = displayName;
        this.price = price;
        this.season = season;
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

    @Override
    public @Nullable Season season() {
        return season;
    }

    public @NotNull Material material() {
        return material;
    }
}
