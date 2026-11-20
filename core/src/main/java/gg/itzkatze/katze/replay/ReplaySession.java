package gg.itzkatze.katze.replay;

import gg.itzkatze.katze.text.TextArg;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.spectator.PlayerViewGui;
import gg.itzkatze.katze.results.SavedBattle;
import gg.itzkatze.katze.scoreboard.ScoreboardContent;
import gg.itzkatze.katze.scoreboard.ScoreboardManager;
import gg.itzkatze.katze.util.StringUtility;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ReplaySession implements AutoCloseable {
    private final Player viewer;
    private final ReplayPlayback playback;
    private final ReplayScoreboard scoreboard;
    private final ReplayCracks cracks;
    private final Location joiningAt;
    private UUID following;
    private UUID perspective;
    private boolean cameraTransition;
    private volatile boolean closed;
    private int lastControlTick = Integer.MIN_VALUE;

    ReplaySession(Player viewer, ReplayStore.Loaded replay, SavedBattle battle) {
        this(viewer, new ReplayPlayback(replay, battle), replay.startActor(viewer.getUniqueId()).id(), null);
    }

    ReplaySession(Player viewer, ReplaySession existing) {
        this(viewer, existing.playback, existing.perspective, existing.viewer.getLocation());
    }

    private ReplaySession(Player viewer, ReplayPlayback playback, UUID perspective, Location joiningAt) {
        this.viewer = viewer;
        this.playback = playback;
        this.perspective = perspective;
        this.joiningAt = joiningAt;
        scoreboard = new ReplayScoreboard(this);
        cracks = new ReplayCracks(viewer, playback::world);
        playback.addViewer(this);
    }

    void start() {
        playback.initialize();
        Location spawn = joiningAt != null && playback.owns(joiningAt.getWorld()) ? joiningAt.clone().add(0, 1, 0)
                : playback.startPosition(viewer.getUniqueId(), perspective);
        if (!viewer.teleport(spawn)) throw new IllegalStateException("Replay teleport was cancelled");
        viewer.getInventory().clear();
        ReplayHotbar.install(viewer, this);
        viewer.getInventory().setHeldItemSlot(1);
        ScoreboardManager.updateScoreboard(KatzePlayer.adapt(viewer));
    }

    void tick() {
        if (closed) return;
        playback.tick(Bukkit.getCurrentTick());
        refreshPlaybackView();
    }

    void refreshPlaybackView() {
        if (closed || !viewer.isOnline()) return;
        if (playback.seeking()) {
            if (Bukkit.getCurrentTick() % 10 == 0) viewer.sendActionBar(Component.translatable("katze.replay.seeking"));
            return;
        }
        if (following != null) {
            Entity actor = playback.actor(following);
            if (actor != null && viewer.getGameMode() != GameMode.SPECTATOR) viewer.setGameMode(GameMode.SPECTATOR);
            if (actor != null && !viewer.getWorld().equals(actor.getWorld())) viewer.teleport(actor.getLocation());
            if (actor != null && viewer.getSpectatorTarget() != actor) cameraTarget(actor);
        }
        cracks.render(playback.mining(), Bukkit.getCurrentTick());
        if (Bukkit.getCurrentTick() % 10 == 0) {
            Component progress = Component.translatable("katze.replay.progress", TextArg.text("time", StringUtility.formatTime(seconds())),
                    TextArg.text("duration", StringUtility.formatTime(duration() / 20)), TextArg.text("speed", Double.toString(speed())));
            if (following != null) progress = progress.append(Component.text(" · ")).append(Component.translatable("katze.replay.camera_exit"));
            viewer.sendActionBar(progress);
        }
    }

    void prepareSeek() { cameraTarget(null); cracks.clear(); }
    void relocateAfterSeek() {
        if (following != null) return;
        Entity actor = playback.actor(perspective);
        if (actor == null) {
            for (var player : playback.players()) {
                actor = playback.actor(player.id());
                if (actor != null) { perspective = player.id(); break; }
            }
        }
        if (actor != null && !viewer.getWorld().equals(actor.getWorld())) viewer.teleport(actor.getLocation().add(0, 2, 0));
    }
    void refreshSharedControls() {
        if (closed || !viewer.isOnline()) return;
        ReplayHotbar.install(viewer, this);
        ScoreboardManager.updateScoreboard(KatzePlayer.adapt(viewer));
    }
    boolean sharesPlayback(ReplaySession other) { return playback == other.playback; }
    boolean canJoin() { return !closed && !playback.closed() && !playback.seeking(); }
    void combatUnavailable() { viewer.sendMessage(Component.translatable("katze.replay.combat_unavailable")); }

    public void seek(int seconds) { playback.seek(seconds); }
    public int duration() { return playback.duration(); }
    public int seconds() { return playback.seconds(); }
    public boolean paused() { return playback.paused(); }
    public void togglePause() { playback.togglePause(); }
    public double speed() { return playback.speed(); }
    public void cycleSpeed() { playback.cycleSpeed(); }
    public ScoreboardContent scoreboard() { return scoreboard; }
    SavedBattle battle() { return playback.battle(); }
    UUID perspective() { return perspective; }
    public List<ReplayFrame.Actor> players() { return playback.players(); }

    public void follow(UUID player) {
        cameraTarget(null);
        following = player;
        if (player != null) perspective = player;
        if (player == null) {
            viewer.setGameMode(GameMode.ADVENTURE);
            viewer.setAllowFlight(true);
            viewer.setFlying(true);
            ReplayHotbar.install(viewer, this);
        }
        ScoreboardManager.updateScoreboard(KatzePlayer.adapt(viewer));
    }
    public boolean following() { return following != null; }
    boolean changingCamera() { return cameraTransition; }
    private void cameraTarget(Entity target) {
        if (viewer.getGameMode() != GameMode.SPECTATOR) return;
        cameraTransition = true;
        try { viewer.setSpectatorTarget(target); }
        finally { cameraTransition = false; }
    }
    public void showControls() { follow(null); viewer.sendMessage(Component.translatable("katze.replay.hotbar_hint")); }
    public void refreshControls() { ReplayHotbar.install(viewer, this); }

    public void control(ItemStack item) {
        int control = ReplayHotbar.control(item);
        int now = Bukkit.getCurrentTick();
        if (control < 0 || (long) now - lastControlTick < 4) return;
        lastControlTick = now;
        switch (control) {
            case 0 -> seek(seconds() - (viewer.isSneaking() ? 60 : 10));
            case 1 -> togglePause();
            case 2 -> seek(seconds() + (viewer.isSneaking() ? 60 : 10));
            case 3 -> cycleSpeed();
            case 4 -> new ReplayPlayersGUI(this).open(KatzePlayer.adapt(viewer));
            case 5 -> playback.seekCombat();
            case 8 -> ReplayService.get().leave(viewer);
            default -> {}
        }
    }

    public void inspect(Entity clicked) {
        UUID id = playback.playerId(clicked);
        if (id != null) inspect(id);
    }

    public void inspect(UUID player) {
        ReplayFrame.Actor actor = playback.definition(player);
        if (actor == null || !"PLAYER".equals(actor.type())) return;
        perspective = player;
        ScoreboardManager.updateScoreboard(KatzePlayer.adapt(viewer));
        new PlayerViewGui(this, player, actor.name()).open(KatzePlayer.adapt(viewer));
    }

    public ReplayFrame.PlayerState playerState(UUID player) { return playback.player(player); }
    public ItemStack inventoryItem(UUID player, int slot) {
        ReplayFrame.PlayerState state = playerState(player);
        return state == null || slot < 0 || slot >= state.inventory().size() ? null : ReplayItems.decode(state.inventory().get(slot));
    }
    public ItemStack equipment(UUID player, int slot) {
        List<String> items = playback.equipment(player);
        return items == null || slot >= items.size() ? null : ReplayItems.decode(items.get(slot));
    }
    public List<ItemStack> backpack(UUID player) {
        ReplayFrame.PlayerState state = playerState(player);
        return state == null || state.backpack() == null ? null : state.backpack().stream().map(ReplayItems::decode).toList();
    }
    public boolean hasBackpack(UUID player) { return playerState(player) != null && playerState(player).backpack() != null; }
    public List<Component> playerInfo(UUID player) {
        ReplayFrame.PlayerState state = playerState(player);
        if (state == null) return List.of(Component.translatable("katze.replay.player_data_unavailable"));
        List<Component> lines = new ArrayList<>(List.of(
                PlayerViewGui.infoLine("katze.spectator.player_view.info.team", Component.text(state.team() == null ? "none" : state.team().toString())),
                PlayerViewGui.infoLine("katze.spectator.player_view.info.health", Component.text(state.health() + " / " + state.maxHealth())),
                PlayerViewGui.infoLine("katze.spectator.player_view.info.food", Component.text(state.food() + " / 20")),
                PlayerViewGui.infoLine("katze.spectator.player_view.info.level", Component.text(state.level())),
                PlayerViewGui.infoLine("katze.spectator.player_view.info.world", Component.text(state.world()))));
        Entity actor = playback.actor(player);
        if (actor != null) {
            Location at = actor.getLocation();
            lines.add(PlayerViewGui.infoLine("katze.spectator.player_view.info.position", Component.text(at.getBlockX() + ", " + at.getBlockY() + ", " + at.getBlockZ())));
        }
        lines.addAll(state.information().stream().map(line -> ReplayText.decode(line, viewer.locale())).toList());
        return lines;
    }

    @Override
    public void close() throws IOException {
        if (closed) return;
        closed = true;
        try { cracks.clear(); cameraTarget(null); }
        finally { playback.removeViewer(this); }
    }
}
