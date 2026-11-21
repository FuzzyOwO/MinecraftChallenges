package gg.itzkatze.katze.game.entry;

import com.destroystokyo.paper.profile.PlayerProfile;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;
import org.bukkit.Location;
import gg.itzkatze.katze.event.TimerStartEvent;
import java.util.Set;
import org.bukkit.World;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import net.kyori.adventure.text.Component;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.EquipmentSlot;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import java.util.Map;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LimboListenerTest {
    @Test void initialSpawnUsesLimbo() {
        UUID id = UUID.randomUUID();
        World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> { throw new AssertionError(method.getName()); });
        PlayerProfile profile = (PlayerProfile) Proxy.newProxyInstance(PlayerProfile.class.getClassLoader(), new Class<?>[]{PlayerProfile.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getId")) return id;
                    throw new AssertionError(method.getName());
                });
        PlayerConfigurationConnection connection = (PlayerConfigurationConnection) Proxy.newProxyInstance(PlayerConfigurationConnection.class.getClassLoader(), new Class<?>[]{PlayerConfigurationConnection.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getProfile")) return profile;
                    throw new AssertionError(method.getName());
                });
        Location original = new Location(world, 10, 70, 10);
        Location limbo = new Location(world, 0, 190, 0);
        LimboListener listener = new LimboListener(limbo);
        AsyncPlayerSpawnLocationEvent event = new AsyncPlayerSpawnLocationEvent(connection, original, false);
        listener.onInitialSpawn(event);
        assertEquals(190, event.getSpawnLocation().getY());
        assertEquals(10, listener.takePreviousLocation(id).getX());
    }
    private ItemStack taggedCompass() {
        PersistentDataContainer data = (PersistentDataContainer) Proxy.newProxyInstance(PersistentDataContainer.class.getClassLoader(), new Class<?>[]{PersistentDataContainer.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("has")) return true;
                    throw new AssertionError(method.getName());
                });
        ItemMeta meta = (ItemMeta) Proxy.newProxyInstance(ItemMeta.class.getClassLoader(), new Class<?>[]{ItemMeta.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getPersistentDataContainer")) return data;
                    throw new AssertionError(method.getName());
                });
        ItemStack compass = new ItemStack() {
            @Override public ItemStack clone() { return this; }
            @Override public Material getType() { return Material.COMPASS; }
            @Override public boolean hasItemMeta() { return true; }
            @Override public ItemMeta getItemMeta() { return meta; }
        };
        return compass;
    }

    @Test void identifiesTaggedCompass() {
        World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> { throw new AssertionError(method.getName()); });
        assertTrue(new LimboListener(new Location(world, 0, 1, 0)).isCompass(taggedCompass()));
    }

    @Test void compassCannotBeDropped() {
        World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> { throw new AssertionError(method.getName()); });
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> { throw new AssertionError(method.getName()); });
        Item item = (Item) Proxy.newProxyInstance(Item.class.getClassLoader(), new Class<?>[]{Item.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getItemStack")) return taggedCompass();
                    throw new AssertionError(method.getName());
                });
        PlayerDropItemEvent event = new PlayerDropItemEvent(player, item);
        new LimboListener(new Location(world, 0, 1, 0)).onDrop(event);
        assertTrue(event.isCancelled());
    }
    @Test void compassDoesNotOverwriteOccupiedSlot() {
        ItemStack existing = new ItemStack() {
            @Override public Material getType() { return Material.DIAMOND; }
        };
        PlayerInventory inventory = (PlayerInventory) Proxy.newProxyInstance(PlayerInventory.class.getClassLoader(), new Class<?>[]{PlayerInventory.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getItem")) return existing;
                    throw new AssertionError("Inventory was modified: " + method.getName());
                });
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getInventory")) return inventory;
                    throw new AssertionError(method.getName());
                });
        World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> { throw new AssertionError(method.getName()); });
        new LimboListener(new Location(world, 0, 1, 0)).giveCompass(player);
    }
    @Test void numberKeyCannotMoveCompass() {
        World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> { throw new AssertionError(method.getName()); });
        PlayerInventory inventory = (PlayerInventory) Proxy.newProxyInstance(PlayerInventory.class.getClassLoader(), new Class<?>[]{PlayerInventory.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getItem")) return taggedCompass();
                    throw new AssertionError(method.getName());
                });
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getInventory")) return inventory;
                    throw new AssertionError(method.getName());
                });
        InventoryView view = (InventoryView) Proxy.newProxyInstance(InventoryView.class.getClassLoader(), new Class<?>[]{InventoryView.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getPlayer" -> player;
                    case "convertSlot" -> args[0];
                    case "getItem", "getCursor" -> null;
                    default -> throw new AssertionError(method.getName());
                });
        InventoryClickEvent event = new InventoryClickEvent(view, InventoryType.SlotType.CONTAINER, 0, ClickType.NUMBER_KEY, InventoryAction.HOTBAR_SWAP, 8);
        new LimboListener(new Location(world, 0, 1, 0)).onInventoryClick(event);
        assertTrue(event.isCancelled());
    }
    @Test void draggingCompassIsCancelled() {
        World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> { throw new AssertionError(method.getName()); });
        InventoryView view = (InventoryView) Proxy.newProxyInstance(InventoryView.class.getClassLoader(), new Class<?>[]{InventoryView.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("convertSlot")) return args[0];
                    throw new AssertionError(method.getName());
                });
        InventoryDragEvent event = new InventoryDragEvent(view, null, taggedCompass(), false, Map.of(0, taggedCompass()));
        new LimboListener(new Location(world, 0, 1, 0)).onInventoryDrag(event);
        assertTrue(event.isCancelled());
    }
    @Test void limboBlocksCannotBeBroken() {
        World limbo = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getKey")) return new NamespacedKey("katzegames", "limbo");
                    throw new AssertionError(method.getName());
                });
        Block block = (Block) Proxy.newProxyInstance(Block.class.getClassLoader(), new Class<?>[]{Block.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getWorld")) return limbo;
                    throw new AssertionError(method.getName());
                });
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> { throw new AssertionError(method.getName()); });
        BlockBreakEvent event = new BlockBreakEvent(block, player);
        new LimboListener(new Location(limbo, 0, 1, 0)).onBlockBreak(event);
        assertTrue(event.isCancelled());
    }
    @Test void rightClickCompassOpensBrowser() {
        World limbo = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getKey")) return new NamespacedKey("katzegames", "limbo");
                    throw new AssertionError(method.getName());
                });
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getWorld")) return limbo;
                    throw new AssertionError(method.getName());
                });
        AtomicInteger opened = new AtomicInteger();
        LimboListener listener = new LimboListener(new Location(limbo, 0, 1, 0), ignored -> opened.incrementAndGet());
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, taggedCompass(), null, BlockFace.SELF, EquipmentSlot.HAND);
        listener.onInteract(event);
        assertEquals(1, opened.get());
    }
    @Test void compassCannotMoveToOffhand() {
        World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> { throw new AssertionError(method.getName()); });
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> { throw new AssertionError(method.getName()); });
        PlayerSwapHandItemsEvent event = new PlayerSwapHandItemsEvent(player, null, taggedCompass());
        new LimboListener(new Location(world, 0, 1, 0)).onSwap(event);
        assertTrue(event.isCancelled());
    }
    @Test void joinPassesSavedLocationToEntryCoordinator() {
        UUID id = UUID.randomUUID();
        World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> { throw new AssertionError(method.getName()); });
        PlayerProfile profile = (PlayerProfile) Proxy.newProxyInstance(PlayerProfile.class.getClassLoader(), new Class<?>[]{PlayerProfile.class},
                (proxy, method, args) -> { if (method.getName().equals("getId")) return id; throw new AssertionError(method.getName()); });
        PlayerConfigurationConnection connection = (PlayerConfigurationConnection) Proxy.newProxyInstance(PlayerConfigurationConnection.class.getClassLoader(), new Class<?>[]{PlayerConfigurationConnection.class},
                (proxy, method, args) -> { if (method.getName().equals("getProfile")) return profile; throw new AssertionError(method.getName()); });
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> { if (method.getName().equals("getUniqueId")) return id; throw new AssertionError(method.getName()); });
        AtomicReference<Location> captured = new AtomicReference<>();
        LimboListener listener = new LimboListener(new Location(world, 0, 190, 0), ignored -> {}, (joined, location) -> captured.set(location));
        listener.onInitialSpawn(new AsyncPlayerSpawnLocationEvent(connection, new Location(world, 34, 70, 2), false));
        listener.onJoin(new PlayerJoinEvent(player, Component.empty()));
        assertEquals(34, captured.get().getX());
    }
    @Test void timerCannotStartBeforeInitialAdmission() {
        World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> { throw new AssertionError(method.getName()); });
        GameEntryService entries = GameEntryService.get();
        entries.beginRound(UUID.randomUUID(), Set.of(UUID.randomUUID()));
        entries.setRoutingEnabled(true);
        try {
            TimerStartEvent event = new TimerStartEvent(0);
            new LimboListener(new Location(world, 0, 1, 0)).onTimerStart(event);
            assertTrue(event.isCancelled());
        } finally {
            entries.setRoutingEnabled(false);
            entries.reset();
        }
    }
}
