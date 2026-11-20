package gg.itzkatze.katze.util;

import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.gson.Gson;
import gg.itzkatze.katze.KatzeGames;
import gg.itzkatze.katze.gui.KatzeGUI;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.replay.ReplayPlayersGUI;
import gg.itzkatze.katze.results.SavedResultsGUI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public final class PlayerHeadProfiles implements Listener {
    private static final Gson GSON = new Gson();
    private static PlayerHeadProfiles instance;
    private final KatzeGames plugin;
    private final Path path;
    private final PlayerHeadTextures textures = new PlayerHeadTextures();
    private final PlayerHeadRequests requests = new PlayerHeadRequests();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ExecutorService io = Executors.newSingleThreadExecutor(Thread.ofPlatform().name("KatzeGames-player-skins").factory());
    private final AtomicBoolean dirty = new AtomicBoolean();
    private final BukkitTask task;
    private int saveTicks;

    private record Texture(String name, String value, String signature) {}
    private record Profile(List<Texture> properties) {}

    static List<ProfileProperty> readProfile(String body) {
        Profile profile = GSON.fromJson(body, Profile.class);
        return profile == null || profile.properties() == null ? List.of() : profile.properties().stream()
                .map(property -> new ProfileProperty(property.name(), property.value(), property.signature())).toList();
    }

    private PlayerHeadProfiles(KatzeGames plugin) {
        this.plugin = plugin;
        path = plugin.getDataDirectory().toPath().resolve("data/player-skins.json");
        try { textures.load(path); }
        catch (IOException | RuntimeException e) { plugin.getLogger().log(Level.WARNING, "Unable to load cached player skins", e); }
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getOnlinePlayers().forEach(this::remember);
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20, 20);
    }

    public static void install(KatzeGames plugin) { instance = new PlayerHeadProfiles(plugin); }

    static List<ProfileProperty> textures(UUID player) {
        if (instance == null) return List.of();
        Player online = Bukkit.getPlayer(player);
        Collection<ProfileProperty> available = (online == null ? Bukkit.getOfflinePlayer(player).getPlayerProfile() : online.getPlayerProfile()).getProperties();
        if (instance.textures.remember(player, available)) instance.dirty.set(true);
        List<ProfileProperty> properties = instance.textures.get(player);
        if (properties.isEmpty()) instance.requests.request(player, System.currentTimeMillis());
        return properties;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) { remember(event.getPlayer()); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) { remember(event.getPlayer()); }

    private void remember(Player player) {
        if (textures.remember(player.getUniqueId(), player.getPlayerProfile().getProperties())) dirty.set(true);
    }

    private void tick() {
        if (++saveTicks >= 30) {
            saveTicks = 0;
            io.execute(this::flush);
        }
        UUID player = requests.next(System.currentTimeMillis());
        if (player == null) return;
        if (!textures.get(player).isEmpty()) {
            requests.complete(player, 200, System.currentTimeMillis());
            return;
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/"
                + player.toString().replace("-", "") + "?unsigned=false")).timeout(Duration.ofSeconds(15)).GET().build();
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).whenCompleteAsync((response, error) -> {
            int status = error == null ? response.statusCode() : 503;
            if (status == 204) status = 404;
            try {
                if (status == 200) {
                    if (textures.remember(player, readProfile(response.body()))) {
                        dirty.set(true);
                        flush();
                        refreshViews();
                    }
                    if (textures.get(player).isEmpty()) status = 404;
                }
            } catch (RuntimeException e) { status = 503; }
            finally { requests.complete(player, status, System.currentTimeMillis()); }
        }, io);
    }

    private void refreshViews() {
        if (!plugin.isEnabled()) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (var entry : List.copyOf(KatzeGUI.GUI_MAP.entrySet())) {
                KatzeGUI gui = entry.getValue();
                if (!(gui instanceof SavedResultsGUI) && !(gui instanceof ReplayPlayersGUI)) continue;
                Player viewer = Bukkit.getPlayer(entry.getKey());
                if (viewer == null || viewer.getOpenInventory().getTopInventory().getHolder() != gui) continue;
                KatzePlayer player = KatzePlayer.adapt(viewer);
                gui.setItems(player);
                gui.updateInventory(player);
            }
        });
    }

    private void flush() {
        if (!dirty.getAndSet(false)) return;
        try { textures.save(path); }
        catch (IOException e) {
            dirty.set(true);
            plugin.getLogger().log(Level.WARNING, "Unable to persist player skins", e);
        }
    }

    public static void shutdown() {
        PlayerHeadProfiles service = instance;
        if (service == null) return;
        service.task.cancel();
        Bukkit.getOnlinePlayers().forEach(service::remember);
        service.client.shutdownNow();
        service.io.execute(service::flush);
        service.io.shutdown();
        try { service.io.awaitTermination(10, TimeUnit.SECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        instance = null;
    }
}
