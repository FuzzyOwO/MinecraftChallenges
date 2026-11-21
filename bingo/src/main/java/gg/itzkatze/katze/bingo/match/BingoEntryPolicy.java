package gg.itzkatze.katze.bingo.match;

import gg.itzkatze.katze.bingo.pack.BingoResourcePack;
import gg.itzkatze.katze.game.GameWorldManager;
import gg.itzkatze.katze.game.entry.EntryReason;
import gg.itzkatze.katze.game.entry.GameEntryPolicy;
import gg.itzkatze.katze.game.entry.GameEntryService;
import gg.itzkatze.katze.game.entry.LeaveReason;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public final class BingoEntryPolicy implements GameEntryPolicy {
    private final BingoMatchManager matches;
    private final BingoResourcePack pack;

    public BingoEntryPolicy(BingoMatchManager matches, BingoResourcePack pack) {
        this.matches = matches;
        this.pack = pack;
    }

    @Override public int priority() { return 100; }
    @Override public boolean finished() { return matches.ended(); }

    @Override public Admission prepare(UUID playerId, EntryReason reason) {
        BingoMatch match = matches.assigned(playerId);
        if (match == null || match.result != BingoMatch.Result.ACTIVE || !pack.ready(playerId)) {
            return new Admission(false, null, Component.translatable("katze.game.limbo.rejoin_denied"));
        }
        World world = reason == EntryReason.ROUND_START ? GameWorldManager.getWorld() : null;
        return new Admission(true, world == null ? null : world.getSpawnLocation(), null);
    }

    @Override public void admitted(UUID playerId, EntryReason reason) {}
    @Override public void left(UUID playerId, LeaveReason reason) {}

    @Override public List<Location> spectatorTargets() {
        return Bukkit.getOnlinePlayers().stream().filter(player -> GameEntryService.get().isPlaying(player.getUniqueId()))
                .map(Player::getLocation).toList();
    }
}
