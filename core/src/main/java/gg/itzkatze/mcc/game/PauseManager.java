package gg.itzkatze.mcc.game;

import gg.itzkatze.mcc.MinecraftChallenges;
import gg.itzkatze.mcc.challenges.ChallengeManager;
import org.bukkit.Bukkit;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockCookEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PauseManager implements Listener {

    private boolean pauseApplied = false;
    private final Map<String, Integer> randomTickSpeedByWorld = new HashMap<>();
    private final Map<UUID, Location> frozenLocations = new HashMap<>();
    private final Set<UUID> aiDisabledEntities = new HashSet<>();

    public void startStateWatcher() {
        Bukkit.getScheduler().runTaskTimer(MinecraftChallenges.get(), this::updatePauseState, 1L, 5L);
    }

    private void updatePauseState() {
        boolean shouldPause = ChallengeManager.isGameStarted() &&
                !MinecraftChallenges.getTimer().hasHitZero() &&
                !MinecraftChallenges.getTimer().isRunning();
        if (shouldPause && !pauseApplied) {
            applyPause();
        } else if (!shouldPause && pauseApplied) {
            clearPause();
        }
    }

    private void applyPause() {
        pauseApplied = true;
        randomTickSpeedByWorld.clear();

        for (World world : Bukkit.getWorlds()) {
            Integer randomTickSpeed = world.getGameRuleValue(GameRules.RANDOM_TICK_SPEED);
            randomTickSpeedByWorld.put(world.getName(), randomTickSpeed == null ? 20 : randomTickSpeed);

            world.setGameRule(GameRules.RANDOM_TICK_SPEED, 0);
            if (world.getGameRuleValue(GameRules.ADVANCE_TIME) != null) {
                world.setGameRule(GameRules.ADVANCE_TIME, false);
            }
            if (world.getGameRuleValue(GameRules.ADVANCE_WEATHER) != null) {
                world.setGameRule(GameRules.ADVANCE_WEATHER, false);
            }

            for (Entity entity : world.getEntities()) {
                entity.setVelocity(new Vector(0, 0, 0));
                if (entity instanceof LivingEntity livingEntity && !(livingEntity instanceof Player)) {
                    try {
                        if (livingEntity.hasAI()) {
                            livingEntity.setAI(false);
                            aiDisabledEntities.add(livingEntity.getUniqueId());
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
    }

    private void clearPause() {
        pauseApplied = false;

        for (World world : Bukkit.getWorlds()) {
            Integer randomTickSpeed = randomTickSpeedByWorld.get(world.getName());
            world.setGameRule(GameRules.RANDOM_TICK_SPEED, randomTickSpeed == null ? 20 : randomTickSpeed);
            if (world.getGameRuleValue(GameRules.ADVANCE_TIME) != null) {
                world.setGameRule(GameRules.ADVANCE_TIME, true);
            }
            if (world.getGameRuleValue(GameRules.ADVANCE_WEATHER) != null) {
                world.setGameRule(GameRules.ADVANCE_WEATHER, true);
            }

            for (Entity entity : world.getEntities()) {
                restoreEntityAI(entity);
            }
        }

        frozenLocations.clear();
    }

    private void restoreEntityAI(Entity entity) {
        if (!(entity instanceof LivingEntity livingEntity)) {
            return;
        }
        UUID entityId = livingEntity.getUniqueId();
        if (!aiDisabledEntities.contains(entityId)) {
            return;
        }
        try {
            livingEntity.setAI(true);
            aiDisabledEntities.remove(entityId);
        } catch (Throwable ignored) {
        }
    }

    private boolean isPausedGameplay() {
        return pauseApplied && ChallengeManager.isGameStarted();
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!isPausedGameplay()) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        boolean movedPosition = from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ();
        if (!movedPosition) return;

        UUID playerId = event.getPlayer().getUniqueId();
        Location freezeAt = frozenLocations.computeIfAbsent(playerId, id -> from.clone());
        event.setTo(freezeAt);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (isPausedGameplay()) event.setCancelled(true);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (isPausedGameplay()) event.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isPausedGameplay()) event.setCancelled(true);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!isPausedGameplay()) return;
        if (event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onHunger(FoodLevelChangeEvent event) {
        if (isPausedGameplay()) event.setCancelled(true);
    }

    @EventHandler
    public void onFurnaceBurn(FurnaceBurnEvent event) {
        if (isPausedGameplay()) event.setCancelled(true);
    }

    @EventHandler
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        if (isPausedGameplay()) event.setCancelled(true);
    }

    @EventHandler
    public void onBrew(BrewEvent event) {
        if (isPausedGameplay()) event.setCancelled(true);
    }

    @EventHandler
    public void onBlockCook(BlockCookEvent event) {
        if (isPausedGameplay()) event.setCancelled(true);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (pauseApplied || aiDisabledEntities.isEmpty()) {
            return;
        }
        for (Entity entity : event.getChunk().getEntities()) {
            restoreEntityAI(entity);
        }
    }
}
