package gg.itzkatze.katze.mobarmy;

import gg.itzkatze.katze.game.GameWorldManager;
import gg.itzkatze.katze.game.entry.EntryReason;
import gg.itzkatze.katze.game.entry.GameEntryPolicy;
import gg.itzkatze.katze.game.entry.LeaveReason;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

final class MobArmyEntryPolicy implements GameEntryPolicy {
    private final MobArmyGame game;

    MobArmyEntryPolicy(MobArmyGame game) { this.game = game; }
    @Override public int priority() { return 200; }
    @Override public boolean preparing() { return game.preparingArena; }
    @Override public boolean finished() { return game.match != null && game.match.phase == MobArmyMatch.Phase.RESULTS; }

    @Override public Admission prepare(UUID playerId, EntryReason reason) {
        MobArmyMatch.Side side = game.side(playerId);
        if (side == null || side.finished || side.forfeited || finished()) {
            return new Admission(false, null, Component.translatable("katze.game.limbo.rejoin_denied"));
        }
        if (game.match.phase == MobArmyMatch.Phase.COLLECTION) {
            World world = reason == EntryReason.ROUND_START ? GameWorldManager.getWorld() : null;
            return new Admission(true, world == null ? null : world.getSpawnLocation(), null);
        }
        int index = game.match.sides.indexOf(side);
        if (!game.arena.ready(index)) return new Admission(false, null, Component.translatable("katze.mobarmy.arena.preparing"));
        boolean roof = game.match.phase == MobArmyMatch.Phase.CONFIGURATION || side.preparing || side.roofTicks.containsKey(playerId);
        return new Admission(true, roof ? game.arena.roof(index) : game.arena.entrance(index), null);
    }

    @Override public void admitted(UUID playerId, EntryReason reason) {
        if (reason == EntryReason.SPECTATE) return;
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) game.admitted(player, reason == EntryReason.ROUND_START);
    }

    @Override public void left(UUID playerId, LeaveReason reason) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) game.rememberPlayer(player);
    }

    @Override public List<Location> spectatorTargets() {
        return game.watchTargets().stream().map(Player::getLocation).toList();
    }
}
