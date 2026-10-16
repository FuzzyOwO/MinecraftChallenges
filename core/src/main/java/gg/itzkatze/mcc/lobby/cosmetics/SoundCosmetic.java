package gg.itzkatze.mcc.lobby.cosmetics;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum SoundCosmetic implements Cosmetic {

    PLING("pling", "block.note_block.pling", "Pling", 30, Material.NOTE_BLOCK),
    HARP("harp", "block.note_block.harp", "Harp", 30, Material.NOTE_BLOCK),
    CHIME("chime", "block.note_block.chime", "Chime", 30, Material.NOTE_BLOCK),
    BELL("bell", "block.note_block.bell", "Bell", 30, Material.BELL),
    XYLOPHONE("xylophone", "block.note_block.xylophone", "Xylophone", 60, Material.NOTE_BLOCK),
    BIT("bit", "block.note_block.bit", "Bit", 60, Material.NOTE_BLOCK),
    BANJO("banjo", "block.note_block.banjo", "Banjo", 60, Material.NOTE_BLOCK),
    COW_BELL("cow_bell", "block.note_block.cow_bell", "Cow Bell", 60, Material.NOTE_BLOCK),
    IRON_XYLOPHONE("iron_xylophone", "block.note_block.iron_xylophone", "Iron Xylophone", 60, Material.IRON_INGOT),
    PLING_LOW("pling_low", "block.note_block.pling", "Low Pling", 60, Material.NOTE_BLOCK),

    AMETHYST_CHIME("amethyst_chime", "block.amethyst_block.chime", "Amethyst Chime", 100, Material.AMETHYST_SHARD),
    AMETHYST_HIT("amethyst_hit", "block.amethyst_block.hit", "Amethyst Hit", 100, Material.AMETHYST_SHARD),
    EXPERIENCE_ORB("experience_orb", "entity.experience_orb.pickup", "Experience Orb", 100, Material.EXPERIENCE_BOTTLE),
    ITEM_PICKUP("item_pickup", "entity.item.pickup", "Item Pickup", 100, Material.HOPPER),
    BUTTON_CLICK("button_click", "ui.button.click", "Button Click", 100, Material.STONE_BUTTON),
    LEVER_CLICK("lever_click", "block.lever.click", "Lever Click", 100, Material.LEVER),
    ARMOR_EQUIP("armor_equip", "item.armor.equip_iron", "Armor Equip", 100, Material.IRON_CHESTPLATE),

    SNOW_BLOCK("snow_block", "block.snow.hit", "Schneeblock", 100, Material.SNOW_BLOCK),
    FOX("fox", "entity.fox.ambient", "Fuchs", 100, Material.SWEET_BERRIES),
    SNOW_GOLEM("snow_golem", "entity.snow_golem.ambient", "Snowgolem", 100, Material.CARVED_PUMPKIN),
    FROG("frog", "entity.frog.ambient", "Frosch Quark", 100, Material.FROGSPAWN),

    LEVEL_UP("level_up", "entity.player.levelup", "Level Up", 100, Material.EXPERIENCE_BOTTLE),
    FIREWORK_TWINKLE("firework_twinkle", "entity.firework_rocket.twinkle", "Firework Twinkle", 100, Material.FIREWORK_ROCKET),
    VILLAGER_YES("villager_yes", "entity.villager.yes", "Villager Yes", 100, Material.EMERALD),
    EXPERIENCE_BOTTLE("experience_bottle", "entity.experience_bottle.throw", "Experience Bottle", 100, Material.EXPERIENCE_BOTTLE),
    CHICKEN_POP("chicken_pop", "entity.chicken.egg", "Chicken Pop", 100, Material.EGG),
    NOTE_BLOCK_BASS("note_block_bass", "block.note_block.bass", "Bass Note", 100, Material.NOTE_BLOCK),
    NOTE_BLOCK_GUITAR("note_block_guitar", "block.note_block.guitar", "Guitar Note", 100, Material.NOTE_BLOCK),
    NOTE_BLOCK_DIDGERIDOO("note_block_didgeridoo", "block.note_block.didgeridoo", "Didgeridoo", 100, Material.NOTE_BLOCK),
    NOTE_BLOCK_COW_BELL("note_block_cow_bell", "block.note_block.cow_bell", "Cowbell Note", 100, Material.NOTE_BLOCK),

    WITCH_CACKLE("witch_cackle", "entity.witch.celebrate", "Witch Cackle", 200, Material.CAULDRON, Season.HALLOWEEN);

    public static final Sound DEFAULT = Sound.sound(Key.key("entity.experience_orb.pickup"), Sound.Source.PLAYER, 1f, 1.2f);

    private final String id;
    private final Sound sound;
    private final String displayName;
    private final int price;
    private final Material icon;
    private final @Nullable Season season;

    SoundCosmetic(String id, String key, String displayName, int price, Material icon) {
        this(id, key, displayName, price, icon, null);
    }

    SoundCosmetic(String id, String key, String displayName, int price, Material icon, @Nullable Season season) {
        this.id = id;
        this.sound = Sound.sound(Key.key(key), Sound.Source.PLAYER, 1f, 1f);
        this.displayName = displayName;
        this.price = price;
        this.icon = icon;
        this.season = season;
    }

    @Override
    public @NotNull String id() {
        return id;
    }

    @Override
    public @NotNull CosmeticCategory category() {
        return CosmeticCategory.SOUND;
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
        return icon;
    }

    @Override
    public @Nullable Season season() {
        return season;
    }

    public @NotNull Sound sound() {
        return sound;
    }
}
