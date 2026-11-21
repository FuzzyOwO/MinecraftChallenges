package gg.itzkatze.katze.game.entry;

import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;
import gg.itzkatze.katze.game.GameWorldManager;
import gg.itzkatze.katze.lobby.LobbyManager;
import gg.itzkatze.katze.event.TimerStartEvent;
import gg.itzkatze.katze.replay.ReplayService;
import gg.itzkatze.katze.text.Texts;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import gg.itzkatze.katze.player.KatzePlayer;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.EventPriority;
import java.util.function.BiConsumer;
import org.bukkit.inventory.EquipmentSlot;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LimboListener implements Listener {
    private static final NamespacedKey COMPASS_KEY = new NamespacedKey("katzegames", "limbo_compass");
    private volatile Location spawn;
    private final Consumer<Player> browser;
    private final BiConsumer<Player, Location> login;
    private final Map<UUID, Location> previousLocations = new ConcurrentHashMap<>();

    public LimboListener(Location spawn) {
        this(spawn, player -> new ActiveGamesGUI(new CurrentActiveGameProvider(GameEntryService.get()), GameEntryService.get())
                .open(KatzePlayer.adapt(player)));
    }

    LimboListener(Location spawn, Consumer<Player> browser) {
        this(spawn, browser, GameEntryService.get()::onLogin);
    }

    LimboListener(Location spawn, Consumer<Player> browser, BiConsumer<Player, Location> login) {
        this.spawn = spawn.clone();
        this.browser = browser;
        this.login = login;
    }
    @EventHandler
    public void onInitialSpawn(AsyncPlayerSpawnLocationEvent event) {
        UUID playerId = event.getConnection().getProfile().getId();
        if (playerId != null) previousLocations.put(playerId, event.getSpawnLocation());
        Location current = LobbyManager.getSpawn();
        event.setSpawnLocation(current == null ? spawn.clone() : current);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Location previous = takePreviousLocation(player.getUniqueId());
        ReplayService replay = ReplayService.get();
        ReplayService.LimboRecovery recovery = replay == null ? null : replay.recoverForLimbo(player);
        if (recovery != null && !recovery.success()) {
            player.kick(Component.translatable("katze.game.start.recovery_failed"));
            return;
        }
        if (recovery != null && recovery.pendingFile() && recovery.location() != null) {
            GameEntryService.get().acceptRecoveredState(player, recovery.location());
        }
        login.accept(player, recovery != null && recovery.location() != null ? recovery.location() : previous);
        if (recovery != null && recovery.pendingFile()) replay.finishLimboRecovery(player);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent event) {
        GameEntryService.get().onQuit(event.getPlayer());
    }

    public void setSpawn(Location spawn) {
        this.spawn = spawn.clone();
    }
    public void giveCompass(Player player) {
        ItemStack existing = player.getInventory().getItem(8);
        if (existing != null && existing.getType() != Material.AIR && !isCompass(existing)) return;
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        meta.displayName(Component.translatable("katze.game.limbo.compass"));
        meta.getPersistentDataContainer().set(COMPASS_KEY, PersistentDataType.BYTE, (byte) 1);
        compass.setItemMeta(meta);
        player.getInventory().setItem(8, Texts.render(compass, player));
    }
    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (isCompass(event.getItemDrop().getItemStack())) event.setCancelled(true);
    }
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        boolean tagged = isCompass(event.getCurrentItem()) || isCompass(event.getCursor());
        if (event.getWhoClicked() instanceof Player player) {
            int key = event.getHotbarButton();
            if (key >= 0) tagged |= isCompass(player.getInventory().getItem(key));
            if (event.getClick() == ClickType.SWAP_OFFHAND) tagged |= isCompass(player.getInventory().getItemInOffHand());
        }
        if (tagged) event.setCancelled(true);
    }
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (isCompass(event.getOldCursor()) || event.getNewItems().values().stream().anyMatch(this::isCompass)
                || event.getRawSlots().stream().anyMatch(slot -> isCompass(event.getView().getItem(slot)))) {
            event.setCancelled(true);
        }
    }
    @EventHandler
    public void onInventoryMove(InventoryMoveItemEvent event) {
        if (isCompass(event.getItem())) event.setCancelled(true);
    }
    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        event.getDrops().removeIf(this::isCompass);
    }
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (GameWorldManager.isLimbo(event.getBlock().getWorld())) event.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (GameWorldManager.isLimbo(event.getBlock().getWorld())) event.setCancelled(true);
    }
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !event.getAction().isRightClick()
                || !isCompass(event.getItem()) || !GameWorldManager.isLimbo(event.getPlayer().getWorld())) return;
        event.setCancelled(true);
        browser.accept(event.getPlayer());
    }
    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (isCompass(event.getMainHandItem()) || isCompass(event.getOffHandItem())) event.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTimerStart(TimerStartEvent event) {
        GameEntryService entries = GameEntryService.get();
        if (!entries.blocksTimerStart()) return;
        event.setCancelled(true);
        entries.resumePreparedRound();
    }
    public boolean isCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(COMPASS_KEY, PersistentDataType.BYTE);
    }
    Location takePreviousLocation(UUID playerId) {
        return previousLocations.remove(playerId);
    }
}
