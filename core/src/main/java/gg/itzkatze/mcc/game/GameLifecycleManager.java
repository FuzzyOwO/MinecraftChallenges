package gg.itzkatze.mcc.game;

import gg.itzkatze.mcc.MinecraftChallenges;
import gg.itzkatze.mcc.challenges.ChallengeManager;
import gg.itzkatze.mcc.player.MccPlayer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

public class GameLifecycleManager implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        if (ChallengeManager.isGameStarted()) {
            return;
        }

        MccPlayer player = MccPlayer.adapt(event.getPlayer());
        sendToLobby(player);
    }

    @EventHandler
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        if (!ChallengeManager.isGameStarted()) {
            MccPlayer.remove(event.getPlayer().getUniqueId());
        }
    }

    public static void sendToLobby(@NotNull MccPlayer player) {
        if (player.getPlayer() == null) return;

        SpawnManager.createSpawnIfNeeded();
        player.getPlayer().setFoodLevel(20);
        player.getPlayer().setHealth(20);
        player.getPlayer().setSaturation(20);
        player.getPlayer().setFireTicks(0);
        player.getPlayer().getInventory().clear();
        player.getPlayer().setGameMode(GameMode.ADVENTURE);
        player.getPlayer().teleport(SpawnManager.getSpawn());
    }

    public static void startGame() {
        SpawnManager.destroySpawn();

        World world = Bukkit.getWorld("world");
        for (MccPlayer player : MccPlayer.getOnlinePlayers()) {
            if (player.getPlayer() == null) continue;

            player.getPlayer().setGameMode(GameMode.SURVIVAL);
            player.getPlayer().getInventory().clear();
            player.getPlayer().setHealth(20);
            player.getPlayer().setSaturation(20);
            player.getPlayer().setFoodLevel(20);
            player.getPlayer().setFireTicks(0);

            if (world != null) {
                player.getPlayer().teleport(world.getSpawnLocation());
            }

            player.getPlayer().playSound(player.getPlayer(), Sound.EVENT_RAID_HORN, SoundCategory.MASTER, 100, 1);
        }
    }

    public static void resetGame() {
        MinecraftChallenges.getTimer().stop();
        ChallengeManager.resetGameState();
        MccPlayer.removeOfflinePlayers();

        for (MccPlayer player : MccPlayer.getOnlinePlayers()) {
            sendToLobby(player);
        }
    }
}
