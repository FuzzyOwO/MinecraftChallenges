package gg.itzkatze.mcc.lobby.cosmetics;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum MusicCosmetic implements Cosmetic {

    DISC_13("disc_13", "music_disc.13", "13", 30, Material.MUSIC_DISC_13, 178),
    CAT("cat", "music_disc.cat", "Cat", 30, Material.MUSIC_DISC_CAT, 185),
    BLOCKS("blocks", "music_disc.blocks", "Blocks", 30, Material.MUSIC_DISC_BLOCKS, 345),
    CHIRP("chirp", "music_disc.chirp", "Chirp", 30, Material.MUSIC_DISC_CHIRP, 185),
    FAR("far", "music_disc.far", "Far", 30, Material.MUSIC_DISC_FAR, 174),
    MALL("mall", "music_disc.mall", "Mall", 30, Material.MUSIC_DISC_MALL, 197),
    MELLOHI("mellohi", "music_disc.mellohi", "Mellohi", 30, Material.MUSIC_DISC_MELLOHI, 96),
    STAL("stal", "music_disc.stal", "Stal", 30, Material.MUSIC_DISC_STAL, 150),
    STRAD("strad", "music_disc.strad", "Strad", 30, Material.MUSIC_DISC_STRAD, 188),
    DISC_11("disc_11", "music_disc.11", "11", 30, Material.MUSIC_DISC_11, 71),
    WARD("ward", "music_disc.ward", "Ward", 30, Material.MUSIC_DISC_WARD, 251),
    WAIT("wait", "music_disc.wait", "Wait", 30, Material.MUSIC_DISC_WAIT, 238),
    OTHERSIDE("otherside", "music_disc.otherside", "Otherside", 60, Material.MUSIC_DISC_OTHERSIDE, 195),
    PIGSTEP("pigstep", "music_disc.pigstep", "Pigstep", 60, Material.MUSIC_DISC_PIGSTEP, 148),
    CREATOR("creator", "music_disc.creator", "Creator", 60, Material.MUSIC_DISC_CREATOR, 178),
    PRECIPICE("precipice", "music_disc.precipice", "Precipice", 60, Material.MUSIC_DISC_PRECIPICE, 300),
    RELIC("relic", "music_disc.relic", "Relic", 60, Material.MUSIC_DISC_RELIC, 218),
    DISC_5("disc_5", "music_disc.5", "5", 60, Material.MUSIC_DISC_5, 178),
    TEARS("tears", "music_disc.tears", "Tears", 60, Material.MUSIC_DISC_TEARS, 175),
    LAVA_CHICKEN("lava_chicken", "music_disc.lava_chicken", "Lava Chicken", 100, Material.MUSIC_DISC_LAVA_CHICKEN, 195),
    BOUNCE("bounce", "music_disc.bounce", "Bounce", 100, Material.MUSIC_DISC_BOUNCE, 235);

    private final String id;
    private final Sound sound;
    private final String displayName;
    private final int price;
    private final Material icon;
    private final int durationSeconds;

    MusicCosmetic(String id, String key, String displayName, int price, Material icon, int durationSeconds) {
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
        this.durationSeconds = durationSeconds;
    }

    public int durationSeconds() {
        return durationSeconds;
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
