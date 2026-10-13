package gg.itzkatze.mcc.lobby.cosmetics;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

public enum SoundCosmetic implements Cosmetic {

    PLING("pling", "block.note_block.pling", "Pling", 30, Material.NOTE_BLOCK),
    HARP("harp", "block.note_block.harp", "Harp", 30, Material.NOTE_BLOCK),
    CHIME("chime", "block.note_block.chime", "Chime", 30, Material.NOTE_BLOCK),
    BELL("bell", "block.note_block.bell", "Bell", 30, Material.BELL),
    XYLOPHONE("xylophone", "block.note_block.xylophone", "Xylophone", 60, Material.NOTE_BLOCK),
    BIT("bit", "block.note_block.bit", "Bit", 60, Material.NOTE_BLOCK),
    BANJO("banjo", "block.note_block.banjo", "Banjo", 60, Material.NOTE_BLOCK),
    COW_BELL("cow_bell", "block.note_block.cow_bell", "Cow Bell", 60, Material.NOTE_BLOCK),
    AMETHYST_CHIME("amethyst_chime", "block.amethyst_block.chime", "Amethyst Chime", 100, Material.AMETHYST_SHARD),
    LEVEL_UP("level_up", "entity.player.levelup", "Level Up", 100, Material.EXPERIENCE_BOTTLE),
    FIREWORK_TWINKLE("firework_twinkle", "entity.firework_rocket.twinkle", "Firework Twinkle", 100, Material.FIREWORK_ROCKET),
    VILLAGER_YES("villager_yes", "entity.villager.yes", "Villager \"Yes\"", 100, Material.EMERALD);

    public static final Sound DEFAULT = Sound.sound(Key.key("entity.experience_orb.pickup"), Sound.Source.PLAYER, 1f, 1.2f);

    private final String id;
    private final Sound sound;
    private final String displayName;
    private final int price;
    private final Material icon;

    SoundCosmetic(String id, String key, String displayName, int price, Material icon) {
        this.id = id;
        this.sound = Sound.sound(Key.key(key), Sound.Source.PLAYER, 1f, 1f);
        this.displayName = displayName;
        this.price = price;
        this.icon = icon;
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

    public @NotNull Sound sound() {
        return sound;
    }
}
