package gg.itzkatze.mcc.data;

import gg.itzkatze.mcc.gui.GUIMaterial;
import lombok.Getter;
import org.bukkit.Material;

import java.util.List;

@Getter
public enum DamageTypeSimplified {
    CONTACT("Contact", new GUIMaterial(Material.CACTUS), List.of(MccDamageType.CACTUS, MccDamageType.CAMPFIRE, MccDamageType.STALAGMITE)),
    ENTITY_ATTACK("Entity Attack", new GUIMaterial(Material.ZOMBIE_HEAD), List.of(MccDamageType.MOB_ATTACK, MccDamageType.MOB_PROJECTILE, MccDamageType.SONIC_BOOM,
            MccDamageType.SPIT, MccDamageType.STING, MccDamageType.TRIDENT, MccDamageType.WITHER, MccDamageType.WIND_CHARGE)),
    PROJECTILE("Projectile", new GUIMaterial(Material.ARROW), List.of(MccDamageType.ARROW, MccDamageType.MOB_PROJECTILE, MccDamageType.TRIDENT,
            MccDamageType.WIND_CHARGE, MccDamageType.DRAGON_BREATH, MccDamageType.FIREBALL)),
    SUFFOCATION("Suffocation", new GUIMaterial(Material.GRAVEL), List.of(MccDamageType.CRAMMING, MccDamageType.IN_WALL)),
    FALL("Fall", new GUIMaterial(Material.FEATHER), List.of(MccDamageType.FALL)),
    FIRE("Fire", new GUIMaterial(Material.FLINT_AND_STEEL), List.of(MccDamageType.IN_FIRE, MccDamageType.ON_FIRE)),
    LAVA("Lava", new GUIMaterial(Material.LAVA_BUCKET), List.of(MccDamageType.LAVA)),
    DROWNING("Drowning", new GUIMaterial(Material.WATER_BUCKET), List.of(MccDamageType.DROWN)),
    EXPLOSION("Explosion", new GUIMaterial(Material.TNT), List.of(MccDamageType.EXPLOSION, MccDamageType.PLAYER_EXPLOSION, MccDamageType.FIREWORKS,
            MccDamageType.FIREBALL)),
    FALLING_BLOCK("Falling Block", new GUIMaterial(Material.POINTED_DRIPSTONE), List.of(MccDamageType.FALLING_ANVIL, MccDamageType.FALLING_BLOCK,
            MccDamageType.FALLING_STALACTITE)),
    HOT_FLOOR("Hot Floor", new GUIMaterial(Material.MAGMA_BLOCK), List.of(MccDamageType.HOT_FLOOR)),
    KINETIC_ENERGY("Kinetic Energy", new GUIMaterial(Material.ELYTRA), List.of(MccDamageType.FLY_INTO_WALL, MccDamageType.ENDER_PEARL)),
    NATURAL("Natural", new GUIMaterial(Material.POWDER_SNOW_BUCKET), List.of(MccDamageType.FREEZE, MccDamageType.LIGHTNING_BOLT, MccDamageType.SWEET_BERRY_BUSH)),
    ;

    private final String name;
    private final GUIMaterial guiMaterial;
    private final List<MccDamageType> damageTypes;

    DamageTypeSimplified(String name, GUIMaterial guiMaterial, List<MccDamageType> damageTypes) {
        this.name = name;
        this.guiMaterial = guiMaterial;
        this.damageTypes = damageTypes;
    }
}
