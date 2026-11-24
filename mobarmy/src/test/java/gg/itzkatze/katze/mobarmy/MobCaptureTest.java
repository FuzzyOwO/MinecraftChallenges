package gg.itzkatze.katze.mobarmy;

import com.google.gson.Gson;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.UnsafeValues;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.PiglinBrute;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class MobCaptureTest {
    @Test void identityAndDamageDoNotSplitOtherwiseIdenticalVariants() {
        assertEquals(MobCapture.canonical("{id:'minecraft:drowned',UUID:[I;1,2,3,4],Health:2f,Pos:[1d,2d,3d],Age:-10,Equipment:{hand:'trident'}}"),
                MobCapture.canonical("{Equipment:{hand:'trident'},Age:-10,Health:20f,UUID:[I;5,6,7,8],id:'minecraft:drowned',Pos:[8d,7d,6d]}"));
    }
    @Test void equipmentAndNestedDataRemainPartOfTheVariant() {
        assertNotEquals(MobCapture.canonical("{id:'minecraft:drowned',Equipment:{hand:'trident'}}"), MobCapture.canonical("{id:'minecraft:drowned',Equipment:{hand:'air'}}"));
        String nbt = "{Health:4f,custom_name:'A, {quoted} \\\"name\\\"',attributes:[{id:'minecraft:max_health',base:40d}]}";
        assertTrue(MobCapture.canonical(nbt).contains("base:40d"));
        assertTrue(MobCapture.canonical(nbt).contains("A, {quoted}"));
    }

    @Test void capturedMobPersistsTheFullItemsInTheirOriginalSlots() {
        ItemStack mainHand = item(Material.TRIDENT, new byte[]{1, 2});
        ItemStack offHand = item(Material.SHIELD, new byte[]{3, 4});
        ItemStack helmet = item(Material.IRON_HELMET, new byte[]{5, 6});
        Map<EquipmentSlot, ItemStack> items = Map.of(EquipmentSlot.HAND, mainHand, EquipmentSlot.OFF_HAND, offHand,
                EquipmentSlot.HEAD, helmet);
        EntityEquipment equipment = proxy(EntityEquipment.class, (instance, method, arguments) -> {
            assertEquals("getItem", method.getName());
            return items.get(arguments[0]);
        });
        MobArmyMatch.CapturedMob mob = new MobArmyMatch.CapturedMob();
        MobCapture.captureEquipment(mob, equipment);

        Gson gson = new Gson();
        MobArmyMatch.CapturedMob restored = gson.fromJson(gson.toJson(mob), MobArmyMatch.CapturedMob.class);
        assertEquals(Map.of(EquipmentSlot.HAND, "AQI=", EquipmentSlot.OFF_HAND, "AwQ=", EquipmentSlot.HEAD, "BQY="),
                restored.equipmentItems);
    }

    @Test void restoredPiglinBruteCannotStartAConversionThatTakesItsGear() throws ReflectiveOperationException {
        AtomicBoolean immune = new AtomicBoolean();
        PiglinBrute brute = proxy(PiglinBrute.class, (instance, method, arguments) -> switch (method.getName()) {
            case "setImmuneToZombification" -> { immune.set((boolean) arguments[0]); yield null; }
            case "setPersistent", "setRemoveWhenFarAway", "setTarget", "setAware", "setAI" -> null;
            case "spawnAt" -> {
                assertTrue(immune.get());
                yield true;
            }
            default -> throw new AssertionError(method.getName());
        });

        withUnsafe((instance, method, arguments) -> {
            assertEquals("deserializeEntity", method.getName());
            return brute;
        }, () -> assertSame(brute, MobCapture.spawn("AQ==", (World) null, false, entity -> new Location(null, 0, 0, 0))));
    }

    private static ItemStack item(Material material, byte[] bytes) {
        return new ItemStack() {
            @Override public Material getType() { return material; }
            @Override public boolean isEmpty() { return false; }
            @Override public byte[] serializeAsBytes() { return bytes; }
            @Override public Map<Enchantment, Integer> getEnchantments() { return Map.of(); }
        };
    }

    @SuppressWarnings({"deprecation", "removal"})
    private static void withUnsafe(InvocationHandler handler, Runnable test) throws ReflectiveOperationException {
        UnsafeValues unsafe = proxy(UnsafeValues.class, handler);
        Server server = proxy(Server.class, (instance, method, arguments) -> {
            assertEquals("getUnsafe", method.getName());
            return unsafe;
        });
        Field field = Bukkit.class.getDeclaredField("server");
        field.setAccessible(true);
        Object previous = field.get(null);
        field.set(null, server);
        try {
            test.run();
        } finally {
            field.set(null, previous);
        }
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }
}
