package gg.itzkatze.mcc.lobby.cosmetics;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum MusicCosmetic implements Cosmetic {

    DISC_13("disc_13", "music_disc.13", "13", 30, Material.MUSIC_DISC_13),
    CAT("cat", "music_disc.cat", "Cat", 30, Material.MUSIC_DISC_CAT),
    BLOCKS("blocks", "music_disc.blocks", "Blocks", 30, Material.MUSIC_DISC_BLOCKS),
    CHIRP("chirp", "music_disc.chirp", "Chirp", 30, Material.MUSIC_DISC_CHIRP),
    FAR("far", "music_disc.far", "Far", 30, Material.MUSIC_DISC_FAR),
    MALL("mall", "music_disc.mall", "Mall", 30, Material.MUSIC_DISC_MALL),
    MELLOHI("mellohi", "music_disc.mellohi", "Mellohi", 30, Material.MUSIC_DISC_MELLOHI),
    STAL("stal", "music_disc.stal", "Stal", 30, Material.MUSIC_DISC_STAL),
    STRAD("strad", "music_disc.strad", "Strad", 30, Material.MUSIC_DISC_STRAD),
    DISC_11("disc_11", "music_disc.11", "11", 30, Material.MUSIC_DISC_11),
    WARD("ward", "music_disc.ward", "Ward", 30, Material.MUSIC_DISC_WARD),
    WAIT("wait", "music_disc.wait", "Wait", 30, Material.MUSIC_DISC_WAIT),
    OTHERSIDE("otherside", "music_disc.otherside", "Otherside", 60, Material.MUSIC_DISC_OTHERSIDE),
    PIGSTEP("pigstep", "music_disc.pigstep", "Pigstep", 60, Material.MUSIC_DISC_PIGSTEP),
    CREATOR("creator", "music_disc.creator", "Creator", 60, Material.MUSIC_DISC_CREATOR),
    PRECIPICE("precipice", "music_disc.precipice", "Precipice", 60, Material.MUSIC_DISC_PRECIPICE),
    RELIC("relic", "music_disc.relic", "Relic", 60, Material.MUSIC_DISC_RELIC),
    DISC_5("disc_5", "music_disc.5", "5", 60, Material.MUSIC_DISC_5),
    TEARS("tears", "music_disc.tears", "Tears", 60, Material.MUSIC_DISC_TEARS),
    LAVA_CHICKEN("lava_chicken", "music_disc.lava_chicken", "Lava Chicken", 100, Material.MUSIC_DISC_LAVA_CHICKEN),
    BOUNCE("bounce", "music_disc.bounce", "Bounce", 100, Material.MUSIC_DISC_BOUNCE);

    private final String id;
    private final Sound sound;
    private final String displayName;
    private final int price;
    private final Material icon;

    MusicCosmetic(String id, String key, String displayName, int price, Material icon) {
        this.id = id;
        this.sound = Sound.sound(
                Key.key(key),
                Sound.Source.RECORD,
                1f,
                1f
        );
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
        return CosmeticCategory.MUSIC;
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
        return null;
    }

    public @NotNull Sound sound() {
        return sound;
    }
}
