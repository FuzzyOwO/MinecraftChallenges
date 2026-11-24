package gg.itzkatze.katze.lobby;

import gg.itzkatze.katze.KatzeGames;
import gg.itzkatze.katze.game.GameWorldManager;
import gg.itzkatze.katze.level.LevelProgress;
import gg.itzkatze.katze.level.LevelStore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

public final class LobbyExperienceBar implements Listener {
    private final LevelStore levels;

    public LobbyExperienceBar(LevelStore levels) {
        this.levels = levels;
    }

    public void start(KatzeGames plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        levels.addChangeListener(id -> {
            Player player = Bukkit.getPlayer(id);
            if (player != null) refresh(player);
        });
        Bukkit.getScheduler().runTaskTimer(plugin, () -> Bukkit.getOnlinePlayers().forEach(this::refresh), 1L, 20L);
    }

    void refresh(Player player) {
        if (!GameWorldManager.isLimbo(player.getWorld())) return;
        LevelProgress progress = levels.progress(player.getUniqueId());
        float fraction = progress.cost() == 0 ? 1f : (float) progress.into() / progress.cost();
        player.sendExperienceChange(fraction, progress.level());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (GameWorldManager.isLimbo(event.getFrom())) {
            player.sendExperienceChange(player.getExp(), player.getLevel());
        } else {
            refresh(player);
        }
    }
}
