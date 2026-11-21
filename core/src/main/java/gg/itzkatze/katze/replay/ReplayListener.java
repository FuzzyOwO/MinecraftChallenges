package gg.itzkatze.katze.replay;

import gg.itzkatze.katze.KatzeGames;
import gg.itzkatze.katze.game.entry.GameEntryService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import io.papermc.paper.event.player.PlayerArmSwingEvent;
import io.papermc.paper.event.entity.EntityBreakEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.inventory.EquipmentSlot;
import com.destroystokyo.paper.event.player.PlayerStopSpectatingEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;

import java.util.Locale;
import java.util.UUID;

final class ReplayListener implements Listener {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAnimation(PlayerArmSwingEvent event) { ReplayService.get().swing(event.getPlayer().getUniqueId()); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void recordContainer(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK
                && event.useInteractedBlock() != Event.Result.DENY && event.getClickedBlock() != null) {
            ReplayService.get().interactContainer(event.getPlayer(), event.getClickedBlock());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent event) {
        ReplayService.get().stopMining(event.getPlayer().getUniqueId());
        ReplayService.get().disconnect(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (GameEntryService.get().isRoutingEnabled()) return;
        Bukkit.getScheduler().runTask(KatzeGames.get(), () -> ReplayService.get().recover(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!ReplayService.get().reviewing(event.getPlayer().getUniqueId())) return;
        String name = event.getMessage().substring(1).split(" ", 2)[0].toLowerCase(Locale.ROOT);
        if (name.equals("replays") || name.equals("katzegames:replays")) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage(Component.translatable("katze.replay.commands"));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (!ReplayService.get().reviewing(event.getPlayer().getUniqueId()) || !ReplayService.isReplayWorld(event.getFrom().getWorld())
                || ReplayService.isReplayWorld(event.getTo().getWorld())) return;
        UUID round = GameEntryService.get().currentRoundId();
        long generation = GameEntryService.get().generation(event.getPlayer().getUniqueId());
        Bukkit.getScheduler().runTask(KatzeGames.get(), () -> {
            if (!java.util.Objects.equals(round, GameEntryService.get().currentRoundId())
                    || generation != GameEntryService.get().generation(event.getPlayer().getUniqueId())) return;
            if (ReplayService.get().reviewing(event.getPlayer().getUniqueId())) ReplayService.get().leave(event.getPlayer());
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPortal(PlayerPortalEvent event) { if (ReplayService.isReplayWorld(event.getFrom().getWorld())) event.setCancelled(true); }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityPortal(EntityPortalEvent event) { if (ReplayService.isReplayWorld(event.getFrom().getWorld())) event.setCancelled(true); }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityTeleport(EntityTeleportEvent event) {
        if (ReplayService.isReplayWorld(event.getFrom().getWorld()) && event.getTo() != null
                && !ReplayService.isReplayWorld(event.getTo().getWorld())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        ReplaySession session = ReplayService.get().session(event.getPlayer().getUniqueId());
        if (session == null) return;
        event.setCancelled(true);
        if (event.getHand() == EquipmentSlot.HAND && event.getAction().isRightClick()) session.control(event.getItem());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        ReplaySession session = ReplayService.get().session(event.getPlayer().getUniqueId());
        if (session == null) return;
        event.setCancelled(true);
        if (event.getHand() == EquipmentSlot.HAND) session.inspect(event.getRightClicked());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) { onInteractEntity(event); }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        ReplaySession session = ReplayService.get().session(player.getUniqueId());
        if (session != null) { event.setCancelled(true); session.inspect(event.getEntity()); }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSwap(PlayerSwapHandItemsEvent event) { if (ReplayService.get().session(event.getPlayer().getUniqueId()) != null) event.setCancelled(true); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent event) {
        ReplaySession session = ReplayService.get().session(event.getPlayer().getUniqueId());
        if (session != null && session.following() && event.isSneaking()) Bukkit.getScheduler().runTask(KatzeGames.get(), () -> {
            if (ReplayService.get() != null && ReplayService.get().session(event.getPlayer().getUniqueId()) == session) session.follow(null);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStopCamera(PlayerStopSpectatingEntityEvent event) {
        ReplaySession session = ReplayService.get().session(event.getPlayer().getUniqueId());
        if (session == null || !session.following() || session.changingCamera()) return;
        Bukkit.getScheduler().runTask(KatzeGames.get(), () -> {
            if (ReplayService.get() != null && ReplayService.get().session(event.getPlayer().getUniqueId()) == session
                    && event.getPlayer().getSpectatorTarget() == null) session.follow(null);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeldItem(PlayerItemHeldEvent event) { ReplayService.get().stopMining(event.getPlayer().getUniqueId()); }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onHunger(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player && ReplayService.get().session(player.getUniqueId()) != null) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void protectBreak(BlockBreakEvent event) { if (ReplayService.get().session(event.getPlayer().getUniqueId()) != null) event.setCancelled(true); }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void protectPlace(BlockPlaceEvent event) { if (ReplayService.get().session(event.getPlayer().getUniqueId()) != null) event.setCancelled(true); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void recordHurt(EntityDamageEvent event) {
        if (event.getFinalDamage() > 0 && event.getEntity() instanceof LivingEntity && !ReplayService.isReplayWorld(event.getEntity().getWorld()))
            ReplayService.get().hurt(event.getEntity().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void recordMining(BlockDamageEvent event) {
        if (!event.getInstaBreak()) ReplayService.get().startMining(event.getPlayer(), event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void stopMining(BlockDamageAbortEvent event) { ReplayService.get().stopMining(event.getPlayer().getUniqueId()); }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrop(PlayerDropItemEvent event) { if (ReplayService.get().session(event.getPlayer().getUniqueId()) != null) event.setCancelled(true); }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPickup(EntityPickupItemEvent event) { if (ReplayService.isReplayWorld(event.getEntity().getWorld())) event.setCancelled(true); }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent event) { if (ReplayService.isReplayWorld(event.getEntity().getWorld())) event.setCancelled(true); }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityBreak(EntityBreakEvent event) { if (ReplayService.isReplayWorld(event.getEntity().getWorld())) event.setCancelled(true); }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTarget(EntityTargetEvent event) { if (ReplayService.isReplayWorld(event.getEntity().getWorld())) event.setCancelled(true); }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onExplosion(EntityExplodeEvent event) { if (ReplayService.isReplayWorld(event.getLocation().getWorld())) event.setCancelled(true); }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPhysics(BlockPhysicsEvent event) { if (ReplayService.isReplayWorld(event.getBlock().getWorld())) event.setCancelled(true); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void recordPhysics(BlockPhysicsEvent event) { ReplayService.get().changed(event.getBlock()); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void recordBreak(BlockBreakEvent event) { ReplayService.get().changed(event.getBlock()); ReplayService.get().stopMining(event.getPlayer().getUniqueId()); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void recordPlace(BlockPlaceEvent event) { ReplayService.get().changed(event.getBlock()); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void recordBurn(BlockBurnEvent event) { ReplayService.get().changed(event.getBlock()); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void recordFade(BlockFadeEvent event) { ReplayService.get().changed(event.getBlock()); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void recordForm(BlockFormEvent event) { ReplayService.get().changed(event.getBlock()); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void recordGrow(BlockGrowEvent event) { ReplayService.get().changed(event.getBlock()); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void recordDecay(LeavesDecayEvent event) { ReplayService.get().changed(event.getBlock()); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void recordEntityBlock(EntityChangeBlockEvent event) { ReplayService.get().changed(event.getBlock()); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void recordExplosion(EntityExplodeEvent event) { event.blockList().forEach(ReplayService.get()::changed); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void recordBlockExplosion(BlockExplodeEvent event) { event.blockList().forEach(ReplayService.get()::changed); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void recordExtend(BlockPistonExtendEvent event) {
        event.getBlocks().forEach(block -> { ReplayService.get().changed(block); ReplayService.get().changed(block.getRelative(event.getDirection())); });
        ReplayService.get().changed(event.getBlock().getRelative(event.getDirection()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void recordRetract(BlockPistonRetractEvent event) {
        event.getBlocks().forEach(block -> { ReplayService.get().changed(block); ReplayService.get().changed(block.getRelative(event.getDirection())); });
        ReplayService.get().changed(event.getBlock().getRelative(event.getDirection()));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFluid(BlockFromToEvent event) { if (ReplayService.isReplayWorld(event.getBlock().getWorld())) event.setCancelled(true); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void recordFluid(BlockFromToEvent event) { ReplayService.get().changed(event.getBlock()); ReplayService.get().changed(event.getToBlock()); }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player && ReplayService.get().session(player.getUniqueId()) != null) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player && ReplayService.get().session(player.getUniqueId()) != null) event.setCancelled(true);
    }
}
