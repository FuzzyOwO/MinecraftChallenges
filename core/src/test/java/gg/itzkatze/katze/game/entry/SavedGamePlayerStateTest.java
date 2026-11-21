package gg.itzkatze.katze.game.entry;

import com.google.gson.Gson;
import org.bukkit.Location;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.GameMode;
import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.Field;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SavedGamePlayerStateTest {
    @Test void capturedPlayerInventoryPassesValidationWithTheCurrentPaperSlotCount() {
        PlayerInventory inventory = (PlayerInventory) Proxy.newProxyInstance(PlayerInventory.class.getClassLoader(), new Class<?>[]{PlayerInventory.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "getContents" -> new ItemStack[43];
                    case "getSize" -> 43;
                    case "getHeldItemSlot" -> 0;
                    default -> throw new AssertionError(method.getName());
                });
        World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("getKey")) return NamespacedKey.minecraft("overworld");
                    throw new AssertionError(method.getName());
                });
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "getInventory" -> inventory;
                    case "getItemOnCursor" -> null;
                    case "getActivePotionEffects" -> List.of();
                    case "getHealth" -> 20d;
                    case "getAbsorptionAmount" -> 0d;
                    case "getFoodLevel" -> 20;
                    case "getSaturation" -> 5f;
                    case "getExhaustion", "getExp" -> 0f;
                    case "getLevel", "getTotalExperience", "getFireTicks", "getFreezeTicks" -> 0;
                    case "getRemainingAir" -> 300;
                    case "getGameMode" -> GameMode.SURVIVAL;
                    case "getLocation" -> new Location(world, 0, 80, 0);
                    default -> throw new AssertionError(method.getName());
                });

        SavedGamePlayerState loadout = SavedGamePlayerState.capture(player);

        assertEquals(43, loadout.inventory.size());
        assertEquals(43, loadout.inventorySize);
        assertDoesNotThrow(loadout::validate);
        Gson gson = new Gson();
        SavedGamePlayerState restored = gson.fromJson(gson.toJson(loadout), SavedGamePlayerState.class);
        assertDoesNotThrow(restored::validate);
        assertEquals(loadout.inventory, restored.inventory);
        assertEquals(loadout.inventorySize, restored.inventorySize);
    }

    @Test void rejectsUnknownSnapshotVersion() {
        SavedGamePlayerState state = new SavedGamePlayerState();
        state.version = 2;
        IllegalStateException error = assertThrows(IllegalStateException.class, state::validate);
        assertTrue(error.getMessage().contains("version"));
    }
    @Test void refusedTeleportDoesNotReplaceInventory() throws Exception {
        World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> { throw new AssertionError(method.getName()); });
        Server server = (Server) Proxy.newProxyInstance(Server.class.getClassLoader(), new Class<?>[]{Server.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getWorld")) return world;
                    throw new AssertionError(method.getName());
                });
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("teleport")) return false;
                    throw new AssertionError("Inventory was touched: " + method.getName());
                });
        SavedGamePlayerState state = new SavedGamePlayerState();
        state.inventory = new ArrayList<>(Collections.nCopies(41, null));
        state.cursor = new ArrayList<>(Collections.singletonList(null));
        state.effects = List.of();
        state.world = "minecraft:overworld";
        state.gameMode = "SURVIVAL";
        state.health = 20;
        Field field = Bukkit.class.getDeclaredField("server");
        field.setAccessible(true);
        Object previous = field.get(null);
        field.set(null, server);
        try {
            assertThrows(IllegalStateException.class, () -> state.restore(player));
        } finally {
            field.set(null, previous);
        }
    }
    @Test void restoringTwiceReplacesInventoryAndPlayerState() throws Exception {
        Map<String, Object> values = new HashMap<>();
        ItemStack[] initial = new ItemStack[43];
        PlayerInventory inventory = (PlayerInventory) Proxy.newProxyInstance(PlayerInventory.class.getClassLoader(), new Class<?>[]{PlayerInventory.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getContents" -> initial;
                    case "getSize" -> 43;
                    case "getHeldItemSlot" -> 3;
                    case "setContents" -> { values.put("inventory", args[0]); yield null; }
                    case "setHeldItemSlot" -> { values.put("slot", args[0]); yield null; }
                    default -> throw new AssertionError(method.getName());
                });
        World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getKey")) return NamespacedKey.minecraft("overworld");
                    throw new AssertionError(method.getName());
                });
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.startsWith("set")) { values.put(name, args[0]); return null; }
                    return switch (name) {
                        case "getInventory" -> inventory;
                        case "getItemOnCursor" -> null;
                        case "getActivePotionEffects" -> List.of();
                        case "getHealth" -> 13d;
                        case "getAbsorptionAmount" -> 2d;
                        case "getFoodLevel" -> 15;
                        case "getSaturation" -> 4f;
                        case "getExhaustion" -> 1f;
                        case "getExp" -> .25f;
                        case "getLevel" -> 7;
                        case "getTotalExperience" -> 80;
                        case "getFireTicks" -> 12;
                        case "getFreezeTicks" -> 8;
                        case "getRemainingAir" -> 150;
                        case "getGameMode" -> GameMode.SURVIVAL;
                        case "getLocation" -> new Location(world, 1, 70, 2, 30, 10);
                        case "teleport" -> { values.put("destination", args[0]); yield true; }
                        case "closeInventory" -> null;
                        default -> throw new AssertionError(name);
                    };
                });
        Server server = (Server) Proxy.newProxyInstance(Server.class.getClassLoader(), new Class<?>[]{Server.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getWorld")) return world;
                    throw new AssertionError(method.getName());
                });
        SavedGamePlayerState captured = SavedGamePlayerState.capture(player);
        SavedGamePlayerState saved = new Gson().fromJson(new Gson().toJson(captured), SavedGamePlayerState.class);
        Field field = Bukkit.class.getDeclaredField("server");
        field.setAccessible(true);
        Object previous = field.get(null);
        field.set(null, server);
        try {
            saved.restore(player);
            saved.restore(player);
        } finally {
            field.set(null, previous);
        }
        assertEquals(43, ((ItemStack[]) values.get("inventory")).length);
        assertEquals(3, values.get("slot"));
        assertEquals(GameMode.SURVIVAL, values.get("setGameMode"));
        assertEquals(13d, values.get("setHealth"));
        assertEquals(2d, values.get("setAbsorptionAmount"));
        assertEquals(15, values.get("setFoodLevel"));
        assertEquals(4f, values.get("setSaturation"));
        assertEquals(1f, values.get("setExhaustion"));
        assertTrue(values.containsKey("setItemOnCursor"));
        assertNull(values.get("setItemOnCursor"));
        assertEquals(7, values.get("setLevel"));
        assertEquals(.25f, values.get("setExp"));
        assertEquals(80, values.get("setTotalExperience"));
        assertEquals(150, values.get("setRemainingAir"));
        assertEquals(12, values.get("setFireTicks"));
        assertEquals(8, values.get("setFreezeTicks"));
        Location destination = (Location) values.get("destination");
        assertEquals(70d, destination.getY());
        assertEquals(30f, destination.getYaw());
    }
}
