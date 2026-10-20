package gg.itzkatze.mcc.spectator;

import gg.itzkatze.mcc.MinecraftChallenges;
import gg.itzkatze.mcc.challenges.ChallengeManager;
import gg.itzkatze.mcc.gui.MccGUI;
import gg.itzkatze.mcc.player.MccPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SpectatorListener implements Listener {

    private static final long VIEW_REFRESH_TICKS = 10;
    private static final Component WELCOME = Component.text(
            "A battle is running, so you are spectating. Right-click a player to see their inventory. Your chat only reaches other spectators.", NamedTextColor.YELLOW);

    // Lowest, so it runs before every listener that creates a wrapper: a player without one was not part of the started game.
    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(@NotNull PlayerJoinEvent event) {
        if (!ChallengeManager.isGameStarted()) return;

        Player player = event.getPlayer();
        if (MccPlayer.adapt(player.getUniqueId()).isEmpty()) {
            SpectatorManager.markLateJoin(player.getUniqueId());
            MccPlayer.adapt(player);
            player.getInventory().clear();
            World world = Bukkit.getWorld("world");
            if (world != null) {
                player.teleport(world.getSpawnLocation());
            }
            player.sendMessage(WELCOME);
        }
        if (SpectatorManager.isSpectator(player.getUniqueId())) {
            SpectatorManager.enterSpectatorMode(player);
        }
    }

    public void startViewRefresh() {
        Bukkit.getScheduler().runTaskTimer(MinecraftChallenges.get(), this::refreshViews, VIEW_REFRESH_TICKS, VIEW_REFRESH_TICKS);
    }

    void refreshViews() {
        for (Map.Entry<UUID, MccGUI> entry : List.copyOf(MccGUI.GUI_MAP.entrySet())) {
            MccGUI gui = entry.getValue();
            if (!(gui instanceof SpectatorView view)) continue;

            Player viewer = Bukkit.getPlayer(entry.getKey());
            if (viewer == null || viewer.getOpenInventory().getTopInventory().getHolder(false) != gui) continue;
            MccPlayer wrapper = MccPlayer.adapt(viewer);
            if (Bukkit.getPlayer(view.targetId()) == null) {
                gui.close(wrapper, MccGUI.CloseReason.SERVER_EXITED);
                viewer.sendMessage(Component.text(view.targetName() + " left the game.", NamedTextColor.GRAY));
            } else {
                gui.updateInventory(wrapper);
            }
        }
    }

    // Paper 26.3 fires PlayerInteractAtEntityEvent for a right-click on an entity, also for spectators; it reaches PlayerInteractEntityEvent listeners.
    @EventHandler
    public void onInteractEntity(@NotNull PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !ChallengeManager.isGameStarted()) return;

        Player viewer = event.getPlayer();
        if (!SpectatorManager.isSpectator(viewer.getUniqueId()) || !(event.getRightClicked() instanceof Player clicked)) return;
        MccPlayer target = MccPlayer.adapt(clicked.getUniqueId()).orElse(null);
        if (target == null || target.isSpectator()) return;

        event.setCancelled(true);
        new PlayerViewGui(target).open(MccPlayer.adapt(viewer));
    }

    @EventHandler
    public void onRespawn(@NotNull PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!ChallengeManager.isGameStarted() || !SpectatorManager.isSpectator(player.getUniqueId())) return;

        Bukkit.getScheduler().runTask(MinecraftChallenges.get(), () -> {
            if (player.isOnline() && ChallengeManager.isGameStarted() && SpectatorManager.isSpectator(player.getUniqueId())) {
                SpectatorManager.enterSpectatorMode(player);
            }
        });
    }
}
